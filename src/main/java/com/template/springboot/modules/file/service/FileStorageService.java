package com.template.springboot.modules.file.service;

import com.template.springboot.modules.file.dto.FileUploadResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    FileUploadResponse save(MultipartFile file, String subfolder);

    Resource load(String subfolder, String filename);

    String detectContentType(String subfolder, String filename);

    void delete(String subfolder, String filename);
}
