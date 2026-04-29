package com.template.springboot.modules.product.dto;

import com.template.springboot.modules.product.entity.Product;
import com.template.springboot.modules.product.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        ProductStatus status,
        String imageUrl,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getSku(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getStock(),
                p.getStatus(),
                p.getImageUrl(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                p.getCreatedBy(),
                p.getUpdatedBy());
    }
}
