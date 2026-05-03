package com.template.springboot.modules.role.mapper;

import com.template.springboot.modules.role.dto.RoleResponse;
import com.template.springboot.modules.role.entity.Role;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleMapper {

    private final ModelMapper modelMapper;

    public RoleResponse toResponse(Role role) {
        return modelMapper.map(role, RoleResponse.class);
    }
}
