package com.template.springboot.modules.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserFilter {

    private String q;
    private String role;
    private Boolean enabled;
}
