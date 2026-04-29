package com.template.springboot.modules.user.mapper;

import com.template.springboot.modules.role.entity.Role;
import com.template.springboot.modules.user.dto.UpdateUserRequest;
import com.template.springboot.modules.user.dto.UserResponse;
import com.template.springboot.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final ModelMapper modelMapper;

    public UserResponse toResponse(User user) {
        Set<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isEnabled(),
                roles,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getCreatedBy(),
                user.getUpdatedBy());
    }

    public void applyUpdate(UpdateUserRequest request, User user) {
        modelMapper.map(request, user);
    }
}
