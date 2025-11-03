package com.annapolislabs.lineage.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base class providing common schema building functionality for MCP tools
 */
public abstract class BaseToolSchemaBuilder {

    protected static final String STRING_TYPE = "string";
    protected static final String DESCRIPTION = "description";

    protected final ObjectMapper objectMapper;

    protected BaseToolSchemaBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Create a base schema object with standard structure
     */
    protected ObjectNode createBaseSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        return schema;
    }

    /**
     * Add a string property to the schema
     */
    protected void addStringProperty(ObjectNode properties, String propertyName, String description) {
        ObjectNode property = properties.putObject(propertyName);
        property.put("type", STRING_TYPE);
        property.put(DESCRIPTION, description);
    }

    /**
     * Add a string property with a default value to the schema
     */
    protected void addStringProperty(ObjectNode properties, String propertyName, String description, String defaultValue) {
        ObjectNode property = properties.putObject(propertyName);
        property.put("type", STRING_TYPE);
        property.put(DESCRIPTION, description);
        property.put("default", defaultValue);
    }

    /**
     * Add required fields to the schema
     */
    protected void addRequiredFields(ObjectNode schema, String... fieldNames) {
        ArrayNode required = schema.putArray("required");
        for (String fieldName : fieldNames) {
            required.add(fieldName);
        }
    }
}
