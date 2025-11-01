package com.annapolislabs.lineage.repository;

import com.annapolislabs.lineage.entity.Requirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequirementRepository extends JpaRepository<Requirement, UUID> {
    // Active (non-deleted) requirements only
    List<Requirement> findByProjectIdAndDeletedAtIsNull(UUID projectId);
    List<Requirement> findByProjectIdAndParentIsNullAndDeletedAtIsNull(UUID projectId);
    List<Requirement> findByParentIdAndDeletedAtIsNull(UUID parentId);
    Optional<Requirement> findByReqIdAndDeletedAtIsNull(String reqId);
    boolean existsByReqIdAndDeletedAtIsNull(String reqId);

    // For version history and ID collision detection (includes deleted)
    List<Requirement> findByProjectId(UUID projectId);
    Optional<Requirement> findByReqId(String reqId);

    @Query(value = "SELECT * FROM requirements WHERE project_id = :projectId AND deleted_at IS NULL AND " +
            "to_tsvector('english', title || ' ' || COALESCE(description, '')) @@ plainto_tsquery('english', :searchQuery)",
            nativeQuery = true)
    List<Requirement> searchByText(@Param("projectId") UUID projectId, @Param("searchQuery") String searchQuery);

    @Query("SELECT r FROM Requirement r WHERE r.project.id = :projectId " +
            "AND r.deletedAt IS NULL " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:priority IS NULL OR r.priority = :priority)")
    List<Requirement> findByFilters(@Param("projectId") UUID projectId,
                                   @Param("status") String status,
                                   @Param("priority") String priority);
}
