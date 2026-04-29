package com.template.springboot.modules.product.specification;

import com.template.springboot.modules.product.dto.ProductFilter;
import com.template.springboot.modules.product.entity.Product;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<Product> withFilter(ProductFilter filter) {
        Specification<Product> spec = Specification.allOf();
        if (filter == null) return spec;

        if (filter.q() != null && !filter.q().isBlank()) {
            String like = "%" + filter.q().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("sku")),  like),
                    cb.like(cb.lower(root.get("name")), like)));
        }
        if (filter.status() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        if (filter.minPrice() != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("price"), filter.minPrice()));
        }
        if (filter.maxPrice() != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("price"), filter.maxPrice()));
        }
        return spec;
    }
}
