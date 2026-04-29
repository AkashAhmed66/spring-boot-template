package com.template.springboot.common.specification;

import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Collection;

public final class SpecificationBuilder {

    private SpecificationBuilder() {}

    public static <T> Specification<T> likeIgnoreCase(String field, String value) {
        if (value == null || value.isBlank()) return null;
        String pattern = "%" + value.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(path(root, field)), pattern);
    }

    public static <T, V> Specification<T> equal(String field, V value) {
        if (value == null) return null;
        return (root, query, cb) -> cb.equal(path(root, field), value);
    }

    public static <T, V> Specification<T> in(String field, Collection<V> values) {
        if (values == null || values.isEmpty()) return null;
        return (root, query, cb) -> path(root, field).in(values);
    }

    public static <T> Specification<T> between(String field, Instant from, Instant to) {
        if (from == null && to == null) return null;
        return (root, query, cb) -> {
            Path<Instant> path = path(root, field);
            if (from != null && to != null) return cb.between(path, from, to);
            if (from != null) return cb.greaterThanOrEqualTo(path, from);
            return cb.lessThanOrEqualTo(path, to);
        };
    }

    @SuppressWarnings("unchecked")
    private static <T, V> Path<V> path(jakarta.persistence.criteria.Root<T> root, String field) {
        if (!field.contains(".")) return (Path<V>) root.get(field);
        String[] parts = field.split("\\.");
        Path<?> p = root.get(parts[0]);
        for (int i = 1; i < parts.length; i++) p = p.get(parts[i]);
        return (Path<V>) p;
    }
}
