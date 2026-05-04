package com.template.springboot.modules.product.controller;

import com.template.springboot.common.dto.ApiResponse;
import com.template.springboot.common.security.CurrentUserService;
import com.template.springboot.common.security.HasPermission;
import com.template.springboot.modules.audit.annotation.Auditable;
import com.template.springboot.modules.permission.enums.PermissionName;
import com.template.springboot.modules.product.dto.ProductFilter;
import com.template.springboot.modules.product.dto.ProductRequest;
import com.template.springboot.modules.product.enums.ProductStatus;
import com.template.springboot.modules.product.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products/Template/Test")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CurrentUserService currentUserService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @HasPermission(PermissionName.PRODUCT_WRITE)
    @Auditable(action = "PRODUCT_CREATE", resourceType = "Product")
    public ApiResponse create(@Valid @RequestPart("data") ProductRequest request,
                              @RequestPart(value = "image", required = false) MultipartFile image) {
        var me = currentUserService.get();
        System.out.println("Current user => id=" + me.getId()
                + ", username=" + me.getUsername()
                + ", email=" + me.getEmail()
                + ", authorities=" + currentUserService.getAuthorities());
        return ApiResponse.created(productService.create(request, image));
    }

    @GetMapping
    @HasPermission(PermissionName.PRODUCT_READ)
    public ApiResponse list(@RequestParam(required = false) String q,
                            @RequestParam(required = false) ProductStatus status,
                            @RequestParam(required = false) BigDecimal minPrice,
                            @RequestParam(required = false) BigDecimal maxPrice,
                            @ParameterObject Pageable pageable) {
        ProductFilter filter = new ProductFilter(q, status, minPrice, maxPrice);
        return new ApiResponse(productService.search(filter, pageable));
    }

    @GetMapping("/{id}")
    @HasPermission(PermissionName.PRODUCT_READ)
    public ApiResponse getById(@PathVariable Long id) {
        return new ApiResponse(productService.getById(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @HasPermission(PermissionName.PRODUCT_WRITE)
    @Auditable(action = "PRODUCT_UPDATE", resourceType = "Product", resourceId = "#id")
    public ApiResponse update(@PathVariable Long id,
                              @Valid @RequestPart("data") ProductRequest request,
                              @RequestPart(value = "image", required = false) MultipartFile image) {
        return new ApiResponse(productService.update(id, request, image), "Updated");
    }

    @DeleteMapping("/{id}")
    @HasPermission(PermissionName.PRODUCT_DELETE)
    @Auditable(action = "PRODUCT_DELETE", resourceType = "Product", resourceId = "#id")
    public ApiResponse delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.message("Deleted");
    }
}
