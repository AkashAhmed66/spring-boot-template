package com.template.springboot.modules.role.mapper;

import com.template.springboot.modules.role.dto.RoleResponse;
import com.template.springboot.modules.role.entity.Role;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    public RoleResponse toResponse(Role role) {
        return RoleResponse.from(role);
    }
}
