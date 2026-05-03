package com.template.springboot.modules.user.specification;

import com.template.springboot.modules.user.dto.UserFilter;
import com.template.springboot.modules.user.entity.User;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {}

    public static Specification<User> withFilter(UserFilter filter) {
        Specification<User> spec = Specification.allOf();
        if (filter == null) return spec;

        if (filter.getQ() != null && !filter.getQ().isBlank()) {
            String like = "%" + filter.getQ().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("username")), like),
                    cb.like(cb.lower(root.get("email")), like),
                    cb.like(cb.lower(cb.coalesce(root.get("firstName"), "")), like),
                    cb.like(cb.lower(cb.coalesce(root.get("lastName"), "")), like)));
        }
        if (filter.getEnabled() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("enabled"), filter.getEnabled()));
        }
        if (filter.getRole() != null && !filter.getRole().isBlank()) {
            spec = spec.and((root, q, cb) -> {
                q.distinct(true);
                return cb.equal(root.join("roles").get("name"), filter.getRole());
            });
        }
        return spec;
    }
}
