package com.template.springboot.modules.product.dto;

import com.template.springboot.modules.product.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilter {

    private String q;
    private ProductStatus status;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
