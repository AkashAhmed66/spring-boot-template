package com.template.springboot.modules.product.serviceImpl;

import com.template.springboot.common.exception.DuplicateResourceException;
import com.template.springboot.common.exception.ResourceNotFoundException;
import com.template.springboot.common.security.SecurityUtils;
import com.template.springboot.modules.file.dto.FileUploadResponse;
import com.template.springboot.modules.file.service.FileStorageService;
import com.template.springboot.modules.product.dto.ProductFilter;
import com.template.springboot.modules.product.dto.ProductRequest;
import com.template.springboot.modules.product.dto.ProductResponse;
import com.template.springboot.modules.product.entity.Product;
import com.template.springboot.modules.product.mapper.ProductMapper;
import com.template.springboot.modules.product.repository.ProductRepository;
import com.template.springboot.modules.product.service.ProductService;
import com.template.springboot.modules.product.specification.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private static final String PRODUCT_IMAGE_SUBFOLDER = "products";

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request, MultipartFile image) {
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("SKU already exists: " + request.sku());
        }
        Product product = productMapper.toEntity(request);
        if (image != null && !image.isEmpty()) {
            FileUploadResponse uploaded = fileStorageService.save(image, PRODUCT_IMAGE_SUBFOLDER);
            product.setImageUrl(uploaded.url());
        }
        Product saved = productRepository.save(product);
        log.info("Product created id={} sku={}", saved.getId(), saved.getSku());
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request, MultipartFile image) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        if (!product.getSku().equals(request.sku()) && productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("SKU already exists: " + request.sku());
        }
        productMapper.applyUpdate(request, product);
        if (image != null && !image.isEmpty()) {
            FileUploadResponse uploaded = fileStorageService.save(image, PRODUCT_IMAGE_SUBFOLDER);
            product.setImageUrl(uploaded.url());
        }
        log.info("Product updated id={}", id);
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return productRepository.findById(id)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> search(ProductFilter filter, Pageable pageable) {
        return productRepository.findAll(ProductSpecifications.withFilter(filter), pageable)
                .map(productMapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.markDeleted(SecurityUtils.getCurrentUsername().orElse("system"));
        productRepository.save(product);
        log.warn("Product soft-deleted id={}", id);
    }
}
