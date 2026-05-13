package com.template.springboot.modules.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private String orderRef;
    private Long productId;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal total;
    private Integer remainingStock;
}
