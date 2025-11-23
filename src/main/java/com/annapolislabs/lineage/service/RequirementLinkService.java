package com.annapolislabs.lineage.service;

import com.annapolislabs.lineage.common.ServiceConstants;
import com.annapolislabs.lineage.dto.request.CreateLinkRequest;
import com.annapolislabs.lineage.dto.response.RequirementResponse;
import com.annapolislabs.lineage.entity.*;
import com.annapolislabs.lineage.exception.AccessDeniedException;
import com.annapolislabs.lineage.exception.InvalidLinkException;
import com.annapolislabs.lineage.exception.ResourceNotFoundException;
import com.annapolislabs.lineage.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service responsible for managing graph-like relationships between requirements,
 * applying validation rules, enforcing authorization, and producing history entries
 * based on the link JavaDoc audit guidance.
 */
@Service
public class RequirementLinkService {

    private final RequirementLinkRepository linkRepository;
    private final RequirementRepository requirementRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final RequirementHistoryRepository historyRepository;
    private final AuthService authService;

    /**
     * Creates the service with all required collaborators for enforcing link rules,
     * authorizing callers, and persisting audit history.
     *
     * @param linkRepository repository used to persist and query requirement links
     * @param requirementRepository repository used to resolve requirement metadata
     * @param projectMemberRepository repository used to validate caller membership/role
     * @param historyRepository repository used to store link change history
     * @param authService service providing the authenticated {@link User}
     */
    @Autowired
    public RequirementLinkService(RequirementLinkRepository linkRepository,
                                 RequirementRepository requirementRepository,
                                 ProjectMemberRepository projectMemberRepository,
                                 RequirementHistoryRepository historyRepository,
                                 AuthService authService) {
        this.linkRepository = linkRepository;
        this.requirementRepository = requirementRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.historyRepository = historyRepository;
        this.authService = authService;
    }

    /**
     * Creates a directional relationship between two requirements while verifying
     * membership, role privileges, level compatibility, and duplicate prevention.
     * The operation runs in a transactional context to persist both the link and
     * symmetric history entries atomically.
     *
     * @param fromRequirementId identifier of the requirement that will be treated as the source
     * @param request payload describing the destination requirement and optional metadata
     * @return serialized representation of the newly created link for immediate client use
     * @throws AccessDeniedException if the current user is not a project member with editor rights
     * @throws InvalidLinkException if requirements share the same level or the link already exists
     * @throws RuntimeException if either participating requirement is missing (should be refined)
     */
    @Transactional
    public Map<String, Object> createLink(UUID fromRequirementId, CreateLinkRequest request) {
        User currentUser = authService.getCurrentUser();

        Requirement fromReq = requirementRepository.findById(fromRequirementId)
                .orElseThrow(() -> new RuntimeException("From requirement not found"));
        Requirement toReq = requirementRepository.findById(request.getToRequirementId())
                .orElseThrow(() -> new RuntimeException("To requirement not found"));

        // Check project access
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(fromReq.getProject().getId(), currentUser.getId())
                .orElseThrow(AccessDeniedException::new);

        if (member.getRole() == ProjectRole.VIEWER) {
            throw new InvalidLinkException("Editor access required");
        }

        // Validate requirements are not on the same level
        if (fromReq.getLevel().equals(toReq.getLevel())) {
            throw new InvalidLinkException("Cannot link requirements on the same level. Links must be between different hierarchical levels.");
        }

        // Check if link already exists (in either direction)
        List<RequirementLink> allLinks = linkRepository.findAllLinksForRequirement(fromRequirementId);
        boolean linkExists = allLinks.stream()
                .anyMatch(link -> {
                    UUID otherReqId = link.getFromRequirement().getId().equals(fromRequirementId)
                        ? link.getToRequirement().getId()
                        : link.getFromRequirement().getId();
                    return otherReqId.equals(request.getToRequirementId());
                });

        if (linkExists) {
            throw new InvalidLinkException("Link already exists");
        }

        RequirementLink link = new RequirementLink(fromReq, toReq, currentUser);
        link = linkRepository.save(link);

        // Note: We don't automatically set parentId here.
        // Links represent the full parent-child relationships.
        // A requirement can have multiple parents through links.
        // The parentId field is just used for the initial/primary parent.

        // Create history entries for both requirements
        createLinkHistoryEntry(fromReq, toReq, currentUser, ChangeType.LINK_ADDED);

        return linkToMap(link);
    }

    /**
     * Retrieves all directional links for the supplied requirement, enforcing that
     * the caller is part of the owning project before projecting each link with a
     * computed "incoming"/"outgoing" direction as defined in the documentation audit.
     * The method is read-only and does not mutate persistence state.
     *
     * @param requirementId identifier of the requirement whose links should be listed
     * @return list of maps describing link metadata (id, direction, counterpart requirement, createdAt)
     * @throws ResourceNotFoundException if the requirement cannot be located for the provided id
     * @throws InvalidLinkException if the current user is not authorized to view the project
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllLinksForRequirement(UUID requirementId) {
        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new ResourceNotFoundException(ServiceConstants.REQUIREMENT_NOT_FOUND));

        User currentUser = authService.getCurrentUser();
        if (!projectMemberRepository.existsByProjectIdAndUserId(requirement.getProject().getId(), currentUser.getId())) {
            throw new InvalidLinkException("Access denied");
        }

        List<RequirementLink> links = linkRepository.findAllLinksForRequirement(requirementId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (RequirementLink link : links) {
            Map<String, Object> linkMap = new HashMap<>();
            linkMap.put("id", link.getId());

            Requirement currentReq = requirement;
            Requirement otherReq;

            if (link.getFromRequirement().getId().equals(requirementId)) {
                otherReq = link.getToRequirement();
            } else {
                otherReq = link.getFromRequirement();
            }

            // Determine direction based on hierarchical levels
            // Out link = pointing DOWN the hierarchy (to lower/child levels)
            // In link = pointing UP the hierarchy (to higher/parent levels)
            if (otherReq.getLevel() > currentReq.getLevel()) {
                // Other requirement is at a lower level (child) - this is an OUT link
                linkMap.put("direction", "outgoing");
            } else {
                // Other requirement is at a higher level (parent) - this is an IN link
                linkMap.put("direction", "incoming");
            }

            linkMap.put("requirement", new RequirementResponse(otherReq));
            linkMap.put("createdAt", link.getCreatedAt());
            result.add(linkMap);
        }

        return result;
    }

    /**
     * Removes a previously created requirement link after verifying that the
     * caller has editor privileges within the same project and that dual
     * history entries are captured before deletion. The removal does not modify
     * either requirement beyond the audit history entry.
     *
     * @param linkId identifier of the persisted {@link RequirementLink} to remove
     * @throws ResourceNotFoundException if no link exists for the provided identifier
     * @throws AccessDeniedException if the caller is not associated with the project
     * @throws InvalidLinkException if the caller lacks sufficient role privileges
     */
    @Transactional
    public void deleteLink(UUID linkId) {
        RequirementLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException(ServiceConstants.LINK_NOT_FOUND));

        User currentUser = authService.getCurrentUser();
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(
                link.getFromRequirement().getProject().getId(), currentUser.getId())
                .orElseThrow(AccessDeniedException::new);

        if (member.getRole() == ProjectRole.VIEWER) {
            throw new InvalidLinkException("Editor access required");
        }

        // Create history entries before deleting
        createLinkHistoryEntry(link.getFromRequirement(), link.getToRequirement(), currentUser, ChangeType.LINK_REMOVED);

        // Delete the link only - don't delete the child requirement
        // The child requirement can have multiple parents through other links
        linkRepository.delete(link);
    }

    /**
     * Projects a {@link RequirementLink} into the lightweight map shape consumed by
     * controllers, ensuring both ends of the relationship and metadata timestamps
     * are captured without exposing internal entities.
     *
     * @param link link entity that was just persisted or retrieved from the repository
     * @return map structure containing ids, serialized requirement summaries, and audit fields
     */
    private Map<String, Object> linkToMap(RequirementLink link) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", link.getId());
        map.put("from", new RequirementResponse(link.getFromRequirement()));
        map.put("to", new RequirementResponse(link.getToRequirement()));
        map.put("createdAt", link.getCreatedAt());
        return map;
    }

    /**
     * Creates symmetric history entries for both participating requirements so that
     * downstream audits can see which requirement initiated or received a link
     * addition/removal along with display metadata for UI presentation.
     *
     * @param fromReq requirement treated as the link source
     * @param toReq requirement treated as the destination
     * @param user authenticated user who performed the change, stored for audit trails
     * @param changeType differentiates whether the entry records a link addition or removal
     */
    private void createLinkHistoryEntry(Requirement fromReq, Requirement toReq, User user, ChangeType changeType) {
        Map<String, Object> linkData = new HashMap<>();
        linkData.put("fromReqId", fromReq.getReqId());
        linkData.put("fromTitle", fromReq.getTitle());
        linkData.put("toReqId", toReq.getReqId());
        linkData.put("toTitle", toReq.getTitle());

        // Create history entries for both requirements
        RequirementHistory fromHistory = new RequirementHistory(fromReq, user, changeType, null, linkData);
        RequirementHistory toHistory = new RequirementHistory(toReq, user, changeType, null, linkData);

        historyRepository.save(fromHistory);
        historyRepository.save(toHistory);
    }
}
