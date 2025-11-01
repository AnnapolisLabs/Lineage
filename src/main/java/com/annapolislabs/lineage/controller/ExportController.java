package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/export")
public class ExportController {

    private final ExportService exportService;

    @Autowired
    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/csv")
    public ResponseEntity<String> exportCsv(@PathVariable UUID projectId) {
        String csv = exportService.exportToCsv(projectId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=requirements.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/json")
    public ResponseEntity<String> exportJson(@PathVariable UUID projectId) {
        String json = exportService.exportToJson(projectId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=requirements.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @GetMapping("/markdown")
    public ResponseEntity<String> exportMarkdown(@PathVariable UUID projectId) {
        String markdown = exportService.exportToMarkdown(projectId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=requirements.md")
                .contentType(MediaType.TEXT_PLAIN)
                .body(markdown);
    }
}
