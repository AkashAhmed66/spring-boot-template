package com.template.springboot.modules.user.serviceImpl;

import com.template.springboot.common.exception.BadRequestException;
import com.template.springboot.common.exception.DuplicateResourceException;
import com.template.springboot.common.exception.ResourceNotFoundException;
import com.template.springboot.common.security.SecurityUtils;
import com.template.springboot.modules.role.entity.Role;
import com.template.springboot.modules.role.repository.RoleRepository;
import com.template.springboot.modules.session.service.UserSessionService;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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

    private static final String USERS_CACHE = "users";
    private static final String USER_DETAILS_CACHE = "userDetails";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final UserSessionService sessionService;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> search(UserFilter filter, Pageable pageable) {
        return userRepository.findAll(UserSpecifications.withFilter(filter), pageable)
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = USERS_CACHE, key = "#id")
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
    @Caching(
            put = @CachePut(value = USERS_CACHE, key = "#id"),
            evict = @CacheEvict(value = USER_DETAILS_CACHE, allEntries = true)
    )
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already in use");
        }
        boolean wasEnabled = user.isEnabled();
        userMapper.applyUpdate(request, user);
        if (wasEnabled && !user.isEnabled()) {
            sessionService.revokeAllForUser(id, "account-deactivated");
        }
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    @Caching(
            put = @CachePut(value = USERS_CACHE, key = "#id"),
            evict = @CacheEvict(value = USER_DETAILS_CACHE, allEntries = true)
    )
    public UserResponse assignRoles(Long id, AssignRolesRequest request) {
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        Set<Role> roles = request.getRoles().stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", name)))
                .collect(Collectors.toCollection(HashSet::new));
        user.setRoles(roles);
        sessionService.revokeAllForUser(id, "roles-changed");
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    @Caching(
            put = @CachePut(value = USERS_CACHE, key = "#id"),
            evict = @CacheEvict(value = USER_DETAILS_CACHE, allEntries = true)
    )
    public UserResponse activate(Long id) {
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (user.isEnabled()) {
            throw new BadRequestException("User is already active");
        }
        user.setEnabled(true);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    @Caching(
            put = @CachePut(value = USERS_CACHE, key = "#id"),
            evict = @CacheEvict(value = USER_DETAILS_CACHE, allEntries = true)
    )
    public UserResponse deactivate(Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (id.equals(currentUserId)) {
            throw new BadRequestException("Cannot deactivate your own account");
        }
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (!user.isEnabled()) {
            throw new BadRequestException("User is already inactive");
        }
        user.setEnabled(false);
        sessionService.revokeAllForUser(id, "account-deactivated");
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void forceLogout(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        sessionService.revokeAllForUser(id, "admin-force-logout");
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = USERS_CACHE, key = "#id"),
            @CacheEvict(value = USER_DETAILS_CACHE, allEntries = true)
    })
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.markDeleted(SecurityUtils.getCurrentUsername().orElse("system"));
        userRepository.save(user);
        sessionService.revokeAllForUser(id, "account-deleted");
    }
}
