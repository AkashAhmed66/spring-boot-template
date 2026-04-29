package com.template.springboot.modules.product.mapper;

import com.template.springboot.modules.product.dto.ProductRequest;
import com.template.springboot.modules.product.dto.ProductResponse;
import com.template.springboot.modules.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final ModelMapper modelMapper;

    public Product toEntity(ProductRequest request) {
        return modelMapper.map(request, Product.class);
    }

    public void applyUpdate(ProductRequest request, Product product) {
        modelMapper.map(request, product);
    }

    public ProductResponse toResponse(Product product) {
        return ProductResponse.from(product);
    }
}
