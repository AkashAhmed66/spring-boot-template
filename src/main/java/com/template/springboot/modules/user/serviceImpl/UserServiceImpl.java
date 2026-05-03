package com.template.springboot.modules.user.serviceImpl;

import com.template.springboot.common.exception.DuplicateResourceException;
import com.template.springboot.common.exception.ResourceNotFoundException;
import com.template.springboot.common.security.SecurityUtils;
import com.template.springboot.modules.role.entity.Role;
import com.template.springboot.modules.role.repository.RoleRepository;
import com.template.springboot.modules.user.dto.AssignRolesRequest;
import com.template.springboot.modules.user.dto.UpdateUserRequest;
import com.template.springboot.modules.user.dto.UserFilter;
import com.template.springboot.modules.user.dto.UserResponse;
import com.template.springboot.modules.user.entity.User;
import com.template.springboot.modules.user.mapper.UserMapper;
import com.template.springboot.modules.user.repository.UserRepository;
import com.template.springboot.modules.user.service.UserService;
import com.template.springboot.modules.user.specification.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> search(UserFilter filter, Pageable pageable) {
        return userRepository.findAll(UserSpecifications.withFilter(filter), pageable)
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userRepository.findWithRolesById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrent() {
        Long id = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        return getById(id);
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already in use");
        }
        userMapper.applyUpdate(request, user);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse assignRoles(Long id, AssignRolesRequest request) {
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        Set<Role> roles = request.getRoles().stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", name)))
                .collect(Collectors.toCollection(HashSet::new));
        user.setRoles(roles);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.markDeleted(SecurityUtils.getCurrentUsername().orElse("system"));
        userRepository.save(user);
    }
}
