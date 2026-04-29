package com.template.springboot.modules.role.serviceImpl;

import com.template.springboot.common.exception.DuplicateResourceException;
import com.template.springboot.common.exception.ResourceNotFoundException;
import com.template.springboot.modules.permission.entity.Permission;
import com.template.springboot.modules.permission.repository.PermissionRepository;
import com.template.springboot.modules.role.dto.AssignPermissionsRequest;
import com.template.springboot.modules.role.dto.RoleFilter;
import com.template.springboot.modules.role.dto.RoleRequest;
import com.template.springboot.modules.role.dto.RoleResponse;
import com.template.springboot.modules.role.entity.Role;
import com.template.springboot.modules.role.mapper.RoleMapper;
import com.template.springboot.modules.role.repository.RoleRepository;
import com.template.springboot.modules.role.service.RoleService;
import com.template.springboot.modules.role.specification.RoleSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    @Override
    @Transactional
    public RoleResponse create(RoleRequest request) {
        if (roleRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Role already exists: " + request.name());
        }
        Role role = new Role();
        role.setName(request.name());
        role.setDescription(request.description());
        role.setPermissions(resolvePermissions(request.permissions()));
        Role saved = roleRepository.save(role);
        log.info("Role created id={} name={} permissions={}", saved.getId(), saved.getName(), saved.getPermissions().size());
        return roleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RoleResponse update(Long id, RoleRequest request) {
        Role role = roleRepository.findWithPermissionsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));
        if (!role.getName().equals(request.name()) && roleRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Role already exists: " + request.name());
        }
        role.setName(request.name());
        role.setDescription(request.description());
        if (request.permissions() != null) {
            role.setPermissions(resolvePermissions(request.permissions()));
        }
        log.info("Role updated id={} name={}", id, request.name());
        return roleMapper.toResponse(role);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getById(Long id) {
        return roleRepository.findWithPermissionsById(id)
                .map(roleMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleResponse> search(RoleFilter filter, Pageable pageable) {
        return roleRepository.findAll(RoleSpecifications.withFilter(filter), pageable)
                .map(roleMapper::toResponse);
    }

    @Override
    @Transactional
    public RoleResponse assignPermissions(Long id, AssignPermissionsRequest request) {
        Role role = roleRepository.findWithPermissionsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", id));
        role.setPermissions(resolvePermissions(request.permissions()));
        log.info("Role {} permissions reassigned ({})", role.getName(), role.getPermissions().size());
        return roleMapper.toResponse(role);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Role", id);
        }
        roleRepository.deleteById(id);
        log.warn("Role deleted id={}", id);
    }

    private Set<Permission> resolvePermissions(Set<String> names) {
        if (names == null || names.isEmpty()) return new HashSet<>();
        return names.stream()
                .map(name -> permissionRepository.findByName(name)
                        .orElseThrow(() -> new ResourceNotFoundException("Permission", name)))
                .collect(Collectors.toCollection(HashSet::new));
    }
}
