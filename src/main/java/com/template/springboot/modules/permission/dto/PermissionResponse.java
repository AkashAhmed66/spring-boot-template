package com.template.springboot.modules.permission.dto;

import com.template.springboot.common.dto.BaseResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse extends BaseResponse {

    private Long id;
    private String name;
    private String description;
}
