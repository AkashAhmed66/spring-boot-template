package com.template.springboot.modules.user.dto;

import com.template.springboot.common.dto.BaseResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse extends BaseResponse {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private Set<String> roles;
}
