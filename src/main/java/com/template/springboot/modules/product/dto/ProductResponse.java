package com.template.springboot.modules.product.dto;

import com.template.springboot.common.dto.BaseResponse;
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
public class ProductResponse extends BaseResponse {

    private Long id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private ProductStatus status;
    private String imageUrl;
}
