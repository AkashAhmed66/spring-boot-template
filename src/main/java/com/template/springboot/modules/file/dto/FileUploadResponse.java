package com.template.springboot.modules.file.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    private String url;
    private String subfolder;
    private String filename;
    private long size;
    private String contentType;
}
