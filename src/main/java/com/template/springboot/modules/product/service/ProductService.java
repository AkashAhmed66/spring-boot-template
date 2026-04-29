package com.template.springboot.modules.product.service;

import com.template.springboot.modules.product.dto.ProductFilter;
import com.template.springboot.modules.product.dto.ProductRequest;
import com.template.springboot.modules.product.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ProductService {

    ProductResponse create(ProductRequest request, MultipartFile image);

    ProductResponse update(Long id, ProductRequest request, MultipartFile image);

    ProductResponse getById(Long id);

    Page<ProductResponse> search(ProductFilter filter, Pageable pageable);

    void delete(Long id);
}
