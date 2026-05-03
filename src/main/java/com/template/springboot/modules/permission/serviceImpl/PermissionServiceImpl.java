package com.template.springboot.modules.permission.serviceImpl;

import com.template.springboot.common.exception.DuplicateResourceException;
import com.template.springboot.common.exception.ResourceNotFoundException;
import com.template.springboot.common.security.SecurityUtils;
import com.template.springboot.modules.permission.dto.PermissionFilter;
import com.template.springboot.modules.permission.dto.PermissionRequest;
import com.template.springboot.modules.permission.dto.PermissionResponse;
import com.template.springboot.modules.permission.entity.Permission;
import com.template.springboot.modules.permission.mapper.PermissionMapper;
import com.template.springboot.modules.permission.repository.PermissionRepository;
import com.template.springboot.modules.permission.service.PermissionService;
import com.template.springboot.modules.permission.specification.PermissionSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Override
    @Transactional
    public PermissionResponse create(PermissionRequest request) {
        if (permissionRepository.findByName(request.getName()).isPresent()) {
            throw new DuplicateResourceException("Permission already exists: " + request.getName());
        }
        Permission saved = permissionRepository.save(permissionMapper.toEntity(request));
        log.info("Permission created id={} name={}", saved.getId(), saved.getName());
        return permissionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public PermissionResponse update(Long id, PermissionRequest request) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", id));
        if (!permission.getName().equals(request.getName())
                && permissionRepository.findByName(request.getName()).isPresent()) {
            throw new DuplicateResourceException("Permission already exists: " + request.getName());
        }
        permissionMapper.applyUpdate(request, permission);
        log.info("Permission updated id={} name={}", id, request.getName());
        return permissionMapper.toResponse(permission);
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse getById(Long id) {
        return permissionRepository.findById(id)
                .map(permissionMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PermissionResponse> search(PermissionFilter filter, Pageable pageable) {
        return permissionRepository.findAll(PermissionSpecifications.withFilter(filter), pageable)
                .map(permissionMapper::toResponse);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", id));
        permission.markDeleted(SecurityUtils.getCurrentUsername().orElse("system"));
        permissionRepository.save(permission);
        log.warn("Permission soft-deleted id={}", id);
    }
}
