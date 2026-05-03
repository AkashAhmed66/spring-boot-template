package com.template.springboot.modules.permission.specification;

import com.template.springboot.modules.permission.dto.PermissionFilter;
import com.template.springboot.modules.permission.entity.Permission;
import org.springframework.data.jpa.domain.Specification;

public final class PermissionSpecifications {

    private PermissionSpecifications() {}

    public static Specification<Permission> withFilter(PermissionFilter filter) {
        Specification<Permission> spec = Specification.allOf();
        if (filter == null) return spec;

        if (filter.getQ() != null && !filter.getQ().isBlank()) {
            String like = "%" + filter.getQ().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(cb.coalesce(root.get("description"), "")), like)));
        }
        return spec;
    }
}
