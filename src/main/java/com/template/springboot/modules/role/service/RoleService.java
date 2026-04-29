package com.template.springboot.modules.role.service;

import com.template.springboot.modules.role.dto.AssignPermissionsRequest;
import com.template.springboot.modules.role.dto.RoleFilter;
import com.template.springboot.modules.role.dto.RoleRequest;
import com.template.springboot.modules.role.dto.RoleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoleService {

    RoleResponse create(RoleRequest request);

    RoleResponse update(Long id, RoleRequest request);

    RoleResponse getById(Long id);

    Page<RoleResponse> search(RoleFilter filter, Pageable pageable);

    RoleResponse assignPermissions(Long id, AssignPermissionsRequest request);

    void delete(Long id);
}
