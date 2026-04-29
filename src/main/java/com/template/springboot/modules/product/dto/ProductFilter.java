package com.template.springboot.modules.product.dto;

import com.template.springboot.modules.product.enums.ProductStatus;

import java.math.BigDecimal;

public record ProductFilter(
        String q,
        ProductStatus status,
        BigDecimal minPrice,
        BigDecimal maxPrice) {
}
