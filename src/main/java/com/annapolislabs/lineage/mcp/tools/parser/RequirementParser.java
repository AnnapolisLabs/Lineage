package com.annapolislabs.lineage.mcp.tools.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Modular requirement parser that extracts structured requirements from text
 */
public class RequirementParser {

    private static final String MEDIUM = "MEDIUM";
    private static final String DRAFT = "DRAFT";
    private static final int MAX_LINE_LENGTH = 10000; // Prevent ReDoS attacks

    private final Pattern reqPattern;
    private final Pattern priorityPattern;
    private final Pattern titlePattern;

    public RequirementParser() {
        // Fixed: Replaced greedy (.+) with possessive (.++) to prevent backtracking
        this.reqPattern = Pattern.compile(
            "^\\s*+(?:[-*•]|\\d+[.)]|REQ[-\\d]+:?|shall|must|should|will)\\s*+(.++)",
            Pattern.CASE_INSENSITIVE
        );
        // Fixed: Made optional group atomic to prevent backtracking
        this.priorityPattern = Pattern.compile(
            "(critical|high|medium|low)(?:\\s++priority)?+:?",
            Pattern.CASE_INSENSITIVE
        );
        // Fixed: Replaced greedy (.+) with possessive (.++) to prevent backtracking
        this.titlePattern = Pattern.compile("^#{1,3}\\s++(.++)$");
    }

    /**
     * Parse text into structured requirements
     */
    public List<Map<String, String>> parse(String text, String context) {
        List<Map<String, String>> requirements = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");

        RequirementBuilder builder = new RequirementBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }

            processLine(trimmedLine, builder, requirements);
        }

        // Add last requirement
        builder.addToListIfNotEmpty(requirements);

        // Fallback: treat entire text as one requirement if no requirements found
        if (requirements.isEmpty() && !text.trim().isEmpty()) {
            String title = "Requirement from " + (context.isEmpty() ? "input" : context);
            requirements.add(createRequirement(title, text.trim(), MEDIUM));
        }

        return requirements;
    }

    private void processLine(String line, RequirementBuilder builder, List<Map<String, String>> requirements) {
        // Truncate excessively long lines to prevent ReDoS
        if (line.length() > MAX_LINE_LENGTH) {
            line = line.substring(0, MAX_LINE_LENGTH);
        }

        // Check for priority indicators
        Matcher priorityMatcher = priorityPattern.matcher(line);
        if (priorityMatcher.find()) {
            builder.setPriority(priorityMatcher.group(1).toUpperCase());
        }

        // Check for markdown headers as titles
        Matcher titleMatcher = titlePattern.matcher(line);
        if (titleMatcher.matches()) {
            builder.setTitle(titleMatcher.group(1));
            return;
        }

        // Check if line starts a requirement
        Matcher reqMatcher = reqPattern.matcher(line);
        if (reqMatcher.matches()) {
            handleNewRequirement(builder, requirements, reqMatcher);
            return;
        }

        // Continue current requirement or treat as standalone
        if (builder.hasContent()) {
            builder.appendDescription(line);
        } else {
            handleStandaloneRequirement(line, builder, requirements);
        }
    }

    private void handleNewRequirement(RequirementBuilder builder, List<Map<String, String>> requirements,
                                      Matcher reqMatcher) {
        // Save previous requirement if exists
        builder.addToListIfNotEmpty(requirements);

        // Extract and clean content
        String content = reqMatcher.group(1).trim();
        content = priorityPattern.matcher(content).replaceFirst("").trim();

        builder.startNew(content);
    }

    private void handleStandaloneRequirement(String line, RequirementBuilder builder,
                                            List<Map<String, String>> requirements) {
        if (line.length() > 20) {
            String cleanLine = priorityPattern.matcher(line).replaceFirst("").trim();
            String title = cleanLine.length() < 100 ? cleanLine : cleanLine.substring(0, 80) + "...";
            requirements.add(createRequirement(title, cleanLine, builder.getCurrentPriority()));
            builder.resetPriority();
        }
    }

    private Map<String, String> createRequirement(String title, String description, String priority) {
        if (title == null || title.isEmpty()) {
            title = "Untitled Requirement";
        }
        if (description == null || description.isEmpty()) {
            description = title;
        }
        return Map.of(
            "title", title,
            "description", description,
            "priority", priority,
            "status", DRAFT
        );
    }

    /**
     * Helper class to build requirements incrementally
     */
    private class RequirementBuilder {
        private StringBuilder description = new StringBuilder();
        private String title;
        private String priority = MEDIUM;

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public String getCurrentPriority() {
            return priority;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void appendDescription(String text) {
            if (!description.isEmpty()) {
                description.append(" ");
            }
            description.append(text);
        }

        public boolean hasContent() {
            return !description.isEmpty() || title != null;
        }

        public void startNew(String content) {
            String capturedPriority = priority;
            reset();
            priority = capturedPriority;

            if (content.length() > 10) {
                extractTitleAndDescription(content);
            } else {
                description.append(content);
            }
        }

        private void extractTitleAndDescription(String content) {
            int dotIndex = content.indexOf('.');
            if (dotIndex > 0 && dotIndex < 80) {
                title = content.substring(0, dotIndex);
                if (dotIndex + 1 < content.length()) {
                    description.append(content.substring(dotIndex + 1).trim());
                }
            } else if (content.length() < 100) {
                title = content;
            } else {
                title = content.substring(0, 80) + "...";
                description.append(content);
            }
        }

        public void addToListIfNotEmpty(List<Map<String, String>> requirements) {
            if (hasContent()) {
                requirements.add(createRequirement(title, description.toString(), priority));
                reset();
            }
        }

        public void reset() {
            description = new StringBuilder();
            title = null;
            priority = MEDIUM;
        }

        public void resetPriority() {
            priority = MEDIUM;
        }
    }
}
