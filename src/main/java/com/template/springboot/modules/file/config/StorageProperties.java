package com.template.springboot.modules.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String basePath) {

    public StorageProperties {
        if (basePath == null || basePath.isBlank()) {
            basePath = "uploads";
        }
    }
}
