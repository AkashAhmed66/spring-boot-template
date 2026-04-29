package com.template.springboot.modules.permission.service;

import com.template.springboot.modules.permission.dto.PermissionFilter;
import com.template.springboot.modules.permission.dto.PermissionRequest;
import com.template.springboot.modules.permission.dto.PermissionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PermissionService {

    PermissionResponse create(PermissionRequest request);

    PermissionResponse update(Long id, PermissionRequest request);

    PermissionResponse getById(Long id);

    Page<PermissionResponse> search(PermissionFilter filter, Pageable pageable);

    void delete(Long id);
}
