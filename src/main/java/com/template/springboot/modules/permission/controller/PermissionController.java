package com.template.springboot.modules.permission.controller;

import com.template.springboot.common.dto.ApiResponse;
import com.template.springboot.common.security.HasPermission;
import com.template.springboot.modules.audit.annotation.Auditable;
import com.template.springboot.modules.permission.dto.PermissionFilter;
import com.template.springboot.modules.permission.dto.PermissionRequest;
import com.template.springboot.modules.permission.enums.PermissionName;
import com.template.springboot.modules.permission.service.PermissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/permissions")
@Tag(name = "Permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    @HasPermission(PermissionName.PERMISSION_WRITE)
    @Auditable(action = "PERMISSION_CREATE", resourceType = "Permission")
    public ApiResponse create(@Valid @RequestBody PermissionRequest request) {
        return ApiResponse.created(permissionService.create(request));
    }

    @GetMapping
    @HasPermission(PermissionName.PERMISSION_READ)
    public ApiResponse list(@RequestParam(required = false) String q,
                            @ParameterObject Pageable pageable) {
        return new ApiResponse(permissionService.search(new PermissionFilter(q), pageable));
    }

    @GetMapping("/{id}")
    @HasPermission(PermissionName.PERMISSION_READ)
    public ApiResponse getById(@PathVariable Long id) {
        return new ApiResponse(permissionService.getById(id));
    }

    @PutMapping("/{id}")
    @HasPermission(PermissionName.PERMISSION_WRITE)
    @Auditable(action = "PERMISSION_UPDATE", resourceType = "Permission", resourceId = "#id")
    public ApiResponse update(@PathVariable Long id, @Valid @RequestBody PermissionRequest request) {
        return new ApiResponse(permissionService.update(id, request), "Permission updated");
    }

    @DeleteMapping("/{id}")
    @HasPermission(PermissionName.PERMISSION_WRITE)
    @Auditable(action = "PERMISSION_DELETE", resourceType = "Permission", resourceId = "#id")
    public ApiResponse delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ApiResponse.message("Permission deleted");
    }
}
