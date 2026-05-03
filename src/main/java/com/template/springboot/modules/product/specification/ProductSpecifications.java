package com.template.springboot.modules.product.specification;

import com.template.springboot.modules.product.dto.ProductFilter;
import com.template.springboot.modules.product.entity.Product;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<Product> withFilter(ProductFilter filter) {
        Specification<Product> spec = Specification.allOf();
        if (filter == null) return spec;

        if (filter.getQ() != null && !filter.getQ().isBlank()) {
            String like = "%" + filter.getQ().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("sku")),  like),
                    cb.like(cb.lower(root.get("name")), like)));
        }
        if (filter.getStatus() != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), filter.getStatus()));
        }
        if (filter.getMinPrice() != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
        }
        if (filter.getMaxPrice() != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
        }
        return spec;
    }
}
