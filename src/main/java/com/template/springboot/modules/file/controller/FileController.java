package com.template.springboot.modules.file.controller;

import com.template.springboot.common.dto.ApiResponse;
import com.template.springboot.modules.file.service.FileStorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "Files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse upload(@RequestPart("file") MultipartFile file,
                              @RequestParam("subfolder") @NotBlank String subfolder) {
        return new ApiResponse(fileStorageService.save(file, subfolder), "File uploaded");
    }

    @GetMapping("/{subfolder}/{filename:.+}")
    public ResponseEntity<Resource> get(@PathVariable String subfolder,
                                        @PathVariable String filename) {
        Resource resource = fileStorageService.load(subfolder, filename);
        String contentType = fileStorageService.detectContentType(subfolder, filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
