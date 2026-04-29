package com.template.springboot.modules.audit.util;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class AuditBodyMasker {

    private static final String MASK = "***";

    private AuditBodyMasker() {}

    /**
     * Best-effort: parse as JSON and replace flagged fields with "***".
     * If the body isn't JSON, return it unchanged (still subject to length truncation).
     */
    public static String mask(String body, ObjectMapper mapper, java.util.List<String> maskedFields) {
        if (body == null || body.isBlank() || maskedFields == null || maskedFields.isEmpty()) return body;
        Set<String> lowered = maskedFields.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        try {
            JsonNode node = mapper.readTree(body);
            walk(node, lowered);
            return mapper.writeValueAsString(node);
        } catch (Exception ex) {
            return body;
        }
    }

    private static void walk(JsonNode node, Set<String> masked) {
        if (node instanceof ObjectNode obj) {
            List<String> names = new ArrayList<>(obj.propertyNames());
            for (String name : names) {
                if (masked.contains(name.toLowerCase(Locale.ROOT))) {
                    obj.put(name, MASK);
                } else {
                    walk(obj.get(name), masked);
                }
            }
        } else if (node.isArray()) {
            node.forEach(child -> walk(child, masked));
        }
    }

    public static String truncate(String body, int max) {
        if (body == null) return null;
        if (body.length() <= max) return body;
        return body.substring(0, max) + "...[truncated]";
    }
}
