package com.annapolislabs.lineage.controller;

import com.annapolislabs.lineage.dto.request.ImportProjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProjectImportHelper {

    private static final Logger logger = LoggerFactory.getLogger(ProjectImportHelper.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new JavaTimeModule());

    private ProjectImportHelper() {
    }

    static ImportProjectRequest parse(String json) {
        try {
            logger.info("Attempting to parse JSON payload of length: {}", json.length());
            logger.debug("JSON payload: {}", json.substring(0, Math.min(json.length(), 500)) + (json.length() > 500 ? "..." : ""));
            
            ImportProjectRequest request = OBJECT_MAPPER.readValue(json, ImportProjectRequest.class);
            logger.info("Successfully parsed JSON - Project name: {}, Requirements count: {}", 
                request.getProject().getName(), 
                request.getRequirements() != null ? request.getRequirements().size() : 0);
            return request;
        } catch (Exception ex) {
            logger.error("Failed to parse JSON payload. Error type: {}, Message: {}", 
                ex.getClass().getSimpleName(), ex.getMessage());
            
            if (ex.getMessage() != null) {
                logger.error("Error details: {}", ex.getMessage());
            }
            
            if (ex.getCause() != null) {
                logger.error("Root cause: {}", ex.getCause().getMessage(), ex.getCause());
            }
            
            throw new IllegalArgumentException("Invalid import payload: " + ex.getMessage(), ex);
        }
    }
}
