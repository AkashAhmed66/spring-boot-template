package com.template.springboot.modules.permission.mapper;

import com.template.springboot.modules.permission.dto.PermissionRequest;
import com.template.springboot.modules.permission.dto.PermissionResponse;
import com.template.springboot.modules.permission.entity.Permission;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionMapper {

    private final ModelMapper modelMapper;

    public Permission toEntity(PermissionRequest request) {
        return modelMapper.map(request, Permission.class);
    }

    public void applyUpdate(PermissionRequest request, Permission permission) {
        modelMapper.map(request, permission);
    }

    public PermissionResponse toResponse(Permission permission) {
        return modelMapper.map(permission, PermissionResponse.class);
    }
}
