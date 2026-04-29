package com.template.springboot.modules.role.controller;

import com.template.springboot.common.dto.ApiResponse;
import com.template.springboot.common.security.HasPermission;
import com.template.springboot.modules.audit.annotation.Auditable;
import com.template.springboot.modules.permission.enums.PermissionName;
import com.template.springboot.modules.role.dto.AssignPermissionsRequest;
import com.template.springboot.modules.role.dto.RoleFilter;
import com.template.springboot.modules.role.dto.RoleRequest;
import com.template.springboot.modules.role.service.RoleService;
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
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @HasPermission(PermissionName.ROLE_WRITE)
    @Auditable(action = "ROLE_CREATE", resourceType = "Role")
    public ApiResponse create(@Valid @RequestBody RoleRequest request) {
        return ApiResponse.created(roleService.create(request));
    }

    @GetMapping
    @HasPermission(PermissionName.ROLE_READ)
    public ApiResponse list(@RequestParam(required = false) String q,
                            @ParameterObject Pageable pageable) {
        return new ApiResponse(roleService.search(new RoleFilter(q), pageable));
    }

    @GetMapping("/{id}")
    @HasPermission(PermissionName.ROLE_READ)
    public ApiResponse getById(@PathVariable Long id) {
        return new ApiResponse(roleService.getById(id));
    }

    @PutMapping("/{id}")
    @HasPermission(PermissionName.ROLE_WRITE)
    @Auditable(action = "ROLE_UPDATE", resourceType = "Role", resourceId = "#id")
    public ApiResponse update(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return new ApiResponse(roleService.update(id, request), "Role updated");
    }

    @PostMapping("/{id}/permissions")
    @HasPermission(PermissionName.ROLE_WRITE)
    @Auditable(action = "ROLE_ASSIGN_PERMISSIONS", resourceType = "Role", resourceId = "#id")
    public ApiResponse assignPermissions(@PathVariable Long id,
                                         @Valid @RequestBody AssignPermissionsRequest request) {
        return new ApiResponse(roleService.assignPermissions(id, request), "Permissions assigned");
    }

    @DeleteMapping("/{id}")
    @HasPermission(PermissionName.ROLE_DELETE)
    @Auditable(action = "ROLE_DELETE", resourceType = "Role", resourceId = "#id")
    public ApiResponse delete(@PathVariable Long id) {
        roleService.delete(id);
        return ApiResponse.message("Role deleted");
    }
}
