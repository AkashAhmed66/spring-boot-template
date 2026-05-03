package com.template.springboot.modules.product.dto;

import com.template.springboot.modules.product.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank
    @Size(max = 50)
    private String sku;

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal price;

    @NotNull
    @PositiveOrZero
    private Integer stock;

    @NotNull
    private ProductStatus status;
}
