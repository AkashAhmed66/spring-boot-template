package com.template.springboot.modules.user.service;

import com.template.springboot.modules.user.dto.AssignRolesRequest;
import com.template.springboot.modules.user.dto.UpdateUserRequest;
import com.template.springboot.modules.user.dto.UserFilter;
import com.template.springboot.modules.user.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    Page<UserResponse> search(UserFilter filter, Pageable pageable);

    UserResponse getById(Long id);

    UserResponse getCurrent();

    UserResponse update(Long id, UpdateUserRequest request);

    UserResponse assignRoles(Long id, AssignRolesRequest request);

    void delete(Long id);
}
