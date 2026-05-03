package com.template.springboot.modules.user.mapper;

import com.template.springboot.modules.user.dto.UpdateUserRequest;
import com.template.springboot.modules.user.dto.UserResponse;
import com.template.springboot.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final ModelMapper modelMapper;

    public UserResponse toResponse(User user) {
        return modelMapper.map(user, UserResponse.class);
    }

    public void applyUpdate(UpdateUserRequest request, User user) {
        modelMapper.map(request, user);
    }
}
