package com.template.springboot.modules.role.specification;

import com.template.springboot.modules.role.dto.RoleFilter;
import com.template.springboot.modules.role.entity.Role;
import org.springframework.data.jpa.domain.Specification;

public final class RoleSpecifications {

    private RoleSpecifications() {}

    public static Specification<Role> withFilter(RoleFilter filter) {
        Specification<Role> spec = Specification.allOf();
        if (filter == null) return spec;

        if (filter.q() != null && !filter.q().isBlank()) {
            String like = "%" + filter.q().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(cb.coalesce(root.get("description"), "")), like)));
        }
        return spec;
    }
}
