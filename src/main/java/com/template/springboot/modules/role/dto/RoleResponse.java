package com.template.springboot.modules.role.dto;

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
public class RoleResponse extends BaseResponse {

    private Long id;
    private String name;
    private String description;
    private Set<String> permissions;
}
