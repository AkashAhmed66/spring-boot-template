package com.template.springboot.modules.user.controller;

import com.template.springboot.common.dto.ApiResponse;
import com.template.springboot.common.security.HasPermission;
import com.template.springboot.modules.audit.annotation.Auditable;
import com.template.springboot.modules.permission.enums.PermissionName;
import com.template.springboot.modules.user.dto.AssignRolesRequest;
import com.template.springboot.modules.user.dto.UpdateUserRequest;
import com.template.springboot.modules.user.dto.UserFilter;
import com.template.springboot.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse me() {
        return new ApiResponse(userService.getCurrent());
    }

    @GetMapping
    @HasPermission(PermissionName.USER_READ)
    public ApiResponse list(@RequestParam(required = false) String q,
                            @RequestParam(required = false) String role,
                            @RequestParam(required = false) Boolean enabled,
                            @ParameterObject Pageable pageable) {
        UserFilter filter = new UserFilter(q, role, enabled);
        return new ApiResponse(userService.search(filter, pageable));
    }

    @GetMapping("/{id}")
    @HasPermission(PermissionName.USER_READ)
    public ApiResponse getById(@PathVariable Long id) {
        return new ApiResponse(userService.getById(id));
    }

    @PutMapping("/{id}")
    @HasPermission(PermissionName.USER_WRITE)
    @Auditable(action = "USER_UPDATE", resourceType = "User", resourceId = "#id")
    public ApiResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return new ApiResponse(userService.update(id, request), "User updated");
    }

    @PostMapping("/{id}/roles")
    @HasPermission(PermissionName.ROLE_WRITE)
    @Auditable(action = "USER_ASSIGN_ROLES", resourceType = "User", resourceId = "#id")
    public ApiResponse assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRolesRequest request) {
        return new ApiResponse(userService.assignRoles(id, request), "Roles assigned");
    }

    @DeleteMapping("/{id}")
    @HasPermission(PermissionName.USER_DELETE)
    @Auditable(action = "USER_DELETE", resourceType = "User", resourceId = "#id")
    public ApiResponse delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.message("User deleted");
    }
}
