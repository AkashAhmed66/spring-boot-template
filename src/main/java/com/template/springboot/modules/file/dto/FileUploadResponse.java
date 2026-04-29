package com.template.springboot.modules.file.dto;

public record FileUploadResponse(
        String url,
        String subfolder,
        String filename,
        long size,
        String contentType) {
}
