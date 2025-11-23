package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller delivering project-level export endpoints for CSV, JSON, and Markdown downloads.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/export")
public class ExportController {

    private final ExportService exportService;

    @Autowired
    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * GET /api/projects/{projectId}/export/csv streams a CSV representation of project requirements.
     * Sets Content-Disposition for download and always returns 200 with the generated document.
     *
     * @param projectId identifier of the project to export
     * @return CSV payload ready for download
     */
    @GetMapping("/csv")
    public ResponseEntity<String> exportCsv(@PathVariable UUID projectId) {
        String csv = exportService.exportToCsv(projectId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=requirements.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    /**
     * GET /api/projects/{projectId}/export/json provides a JSON export suitable for API clients.
     *
     * @param projectId identifier of the project to export
     * @return application/json payload packaged as an attachment
     */
    @GetMapping("/json")
    public ResponseEntity<String> exportJson(@PathVariable UUID projectId) {
        String json = exportService.exportToJson(projectId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=requirements.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    /**
     * GET /api/projects/{projectId}/export/markdown produces a Markdown document summarizing requirements.
     *
     * @param projectId project identifier
     * @return text/plain response with Content-Disposition set for download
     */
    @GetMapping("/markdown")
    public ResponseEntity<String> exportMarkdown(@PathVariable UUID projectId) {
        String markdown = exportService.exportToMarkdown(projectId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=requirements.md")
                .contentType(MediaType.TEXT_PLAIN)
                .body(markdown);
    }
}
