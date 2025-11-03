package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.service.ExportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    @Mock
    private ExportService exportService;

    @InjectMocks
    private ExportController exportController;

    @Test
    void exportCsv_Success() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        String csvContent = "id,title,description\n1,Test,Description";
        when(exportService.exportToCsv(projectId)).thenReturn(csvContent);

        // Act
        ResponseEntity<String> response = exportController.exportCsv(projectId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(csvContent, response.getBody());
        assertTrue(response.getHeaders().getContentDisposition().toString().contains("requirements.csv"));
        verify(exportService).exportToCsv(projectId);
    }

    @Test
    void exportJson_Success() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        String jsonContent = "{\"requirements\": []}";
        when(exportService.exportToJson(projectId)).thenReturn(jsonContent);

        // Act
        ResponseEntity<String> response = exportController.exportJson(projectId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(jsonContent, response.getBody());
        assertTrue(response.getHeaders().getContentDisposition().toString().contains("requirements.json"));
        verify(exportService).exportToJson(projectId);
    }

    @Test
    void exportMarkdown_Success() {
        // Arrange
        UUID projectId = UUID.randomUUID();
        String markdownContent = "# Requirements\n\n## Requirement 1";
        when(exportService.exportToMarkdown(projectId)).thenReturn(markdownContent);

        // Act
        ResponseEntity<String> response = exportController.exportMarkdown(projectId);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(markdownContent, response.getBody());
        assertTrue(response.getHeaders().getContentDisposition().toString().contains("requirements.md"));
        verify(exportService).exportToMarkdown(projectId);
    }
}
