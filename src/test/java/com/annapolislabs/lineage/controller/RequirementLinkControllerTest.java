package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.dto.request.CreateLinkRequest;
import com.annapolislabs.lineage.service.RequirementLinkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequirementLinkControllerTest {

    @Mock
    private RequirementLinkService linkService;

    @InjectMocks
    private RequirementLinkController linkController;

    @Test
    void createLink_Success() {
        // Arrange
        UUID requirementId = UUID.randomUUID();
        UUID linkId = UUID.randomUUID();
        CreateLinkRequest request = new CreateLinkRequest();
        request.setToRequirementId(UUID.randomUUID());

        Map<String, Object> linkResponse = new HashMap<>();
        linkResponse.put("id", linkId);
        linkResponse.put("fromRequirementId", requirementId);
        linkResponse.put("toRequirementId", request.getToRequirementId());

        when(linkService.createLink(eq(requirementId), any(CreateLinkRequest.class)))
                .thenReturn(linkResponse);

        // Act
        ResponseEntity<Map<String, Object>> response = linkController.createLink(requirementId, request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(linkId, response.getBody().get("id"));
        verify(linkService).createLink(eq(requirementId), any(CreateLinkRequest.class));
    }

    @Test
    void getAllLinksForRequirement_Success() {
        // Arrange
        UUID requirementId = UUID.randomUUID();
        List<Map<String, Object>> links = Arrays.asList(
                Map.of("id", UUID.randomUUID(), "type", "parent"),
                Map.of("id", UUID.randomUUID(), "type", "child")
        );

        when(linkService.getAllLinksForRequirement(requirementId)).thenReturn(links);

        // Act
        ResponseEntity<List<Map<String, Object>>> response = linkController.getAllLinksForRequirement(requirementId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(linkService).getAllLinksForRequirement(requirementId);
    }

    @Test
    void deleteLink_Success() {
        // Arrange
        UUID linkId = UUID.randomUUID();
        doNothing().when(linkService).deleteLink(linkId);

        // Act
        ResponseEntity<Void> response = linkController.deleteLink(linkId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(linkService).deleteLink(linkId);
    }
}
