package com.template.springboot.modules.product.serviceImpl;

import com.template.springboot.common.exception.BadRequestException;
import com.template.springboot.common.exception.DuplicateResourceException;
import com.template.springboot.common.exception.ResourceNotFoundException;
import com.template.springboot.common.security.SecurityUtils;
import com.template.springboot.modules.file.dto.FileUploadResponse;
import com.template.springboot.modules.file.service.FileStorageService;
import com.template.springboot.modules.product.dto.OrderResponse;
import com.template.springboot.modules.product.dto.PlaceOrderRequest;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private static final String PRODUCT_IMAGE_SUBFOLDER = "products";
    private static final String PRODUCT_CACHE = "products";

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request, MultipartFile image) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("SKU already exists: " + request.getSku());
        }
        Product product = productMapper.toEntity(request);
        if (image != null && !image.isEmpty()) {
            FileUploadResponse uploaded = fileStorageService.save(image, PRODUCT_IMAGE_SUBFOLDER);
            product.setImageUrl(uploaded.getUrl());
        }
        Product saved = productRepository.save(product);
        log.info("Product created id={} sku={}", saved.getId(), saved.getSku());
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CachePut(value = PRODUCT_CACHE, key = "#id")
    public ProductResponse update(Long id, ProductRequest request, MultipartFile image) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("SKU already exists: " + request.getSku());
        }
        productMapper.applyUpdate(request, product);
        if (image != null && !image.isEmpty()) {
            FileUploadResponse uploaded = fileStorageService.save(image, PRODUCT_IMAGE_SUBFOLDER);
            product.setImageUrl(uploaded.getUrl());
        }
        log.info("Product updated id={}", id);
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = PRODUCT_CACHE, key = "#id")
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
    @CacheEvict(value = PRODUCT_CACHE, key = "#id")
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.markDeleted(SecurityUtils.getCurrentUsername().orElse("system"));
        productRepository.save(product);
        log.warn("Product soft-deleted id={}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = PRODUCT_CACHE, key = "#request.productId")
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));
        if (product.getStock() < request.getQuantity()) {
            throw new BadRequestException("Insufficient stock for product " + product.getSku()
                    + " (available=" + product.getStock() + ", requested=" + request.getQuantity() + ")");
        }
        product.setStock(product.getStock() - request.getQuantity());
        BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        String orderRef = "ORD-" + UUID.randomUUID();
        log.info("Order placed ref={} productId={} qty={} total={}",
                orderRef, product.getId(), request.getQuantity(), total);
        return new OrderResponse(
                orderRef,
                product.getId(),
                product.getSku(),
                request.getQuantity(),
                product.getPrice(),
                total,
                product.getStock());
    }
}
