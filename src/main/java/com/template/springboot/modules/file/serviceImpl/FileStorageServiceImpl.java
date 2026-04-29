package com.template.springboot.modules.file.serviceImpl;

import com.template.springboot.common.exception.BadRequestException;
import com.template.springboot.common.exception.ResourceNotFoundException;
import com.template.springboot.modules.file.config.StorageProperties;
import com.template.springboot.modules.file.dto.FileUploadResponse;
import com.template.springboot.modules.file.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private static final String FILE_API_PATH = "/api/v1/files/";
    private static final Pattern SUBFOLDER_PATTERN = Pattern.compile("^[a-zA-Z0-9_/-]+$");
    private static final Pattern FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

    private final StorageProperties properties;

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(rootPath());
            log.info("File storage initialised at {}", rootPath());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create storage directory: " + rootPath(), e);
        }
    }

    @Override
    public FileUploadResponse save(MultipartFile file, String subfolder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        String safeSubfolder = sanitizeSubfolder(subfolder);
        String extension = extensionOf(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString().replace("-", "")
                + (extension.isEmpty() ? "" : "." + extension);

        Path target = resolve(safeSubfolder, filename);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file " + filename, e);
        }

        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(FILE_API_PATH)
                .path(safeSubfolder + "/" + filename)
                .build()
                .toUriString();

        log.info("Uploaded file subfolder={} filename={} size={}B", safeSubfolder, filename, file.getSize());
        return new FileUploadResponse(url, safeSubfolder, filename, file.getSize(), file.getContentType());
    }

    @Override
    public Resource load(String subfolder, String filename) {
        Path file = resolve(sanitizeSubfolder(subfolder), filename);
        if (!Files.exists(file) || !Files.isReadable(file)) {
            throw new ResourceNotFoundException("File not found: " + subfolder + "/" + filename);
        }
        try {
            return new UrlResource(file.toUri());
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Cannot read file " + filename, e);
        }
    }

    @Override
    public String detectContentType(String subfolder, String filename) {
        Path file = resolve(sanitizeSubfolder(subfolder), filename);
        try {
            String type = Files.probeContentType(file);
            return type != null ? type : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    @Override
    public void delete(String subfolder, String filename) {
        Path file = resolve(sanitizeSubfolder(subfolder), filename);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", file, e.getMessage());
        }
    }

    private Path rootPath() {
        return Paths.get(properties.basePath()).toAbsolutePath().normalize();
    }

    private Path resolve(String subfolder, String filename) {
        if (filename == null || filename.isBlank() || !FILENAME_PATTERN.matcher(filename).matches()) {
            throw new BadRequestException("Invalid filename");
        }
        Path base = rootPath();
        Path resolved = base.resolve(subfolder).resolve(filename).normalize();
        if (!resolved.startsWith(base)) {
            throw new BadRequestException("Invalid file path");
        }
        return resolved;
    }

    private String sanitizeSubfolder(String subfolder) {
        if (subfolder == null || subfolder.isBlank()) return "misc";
        String trimmed = subfolder.trim().toLowerCase(Locale.ROOT);
        if (!SUBFOLDER_PATTERN.matcher(trimmed).matches() || trimmed.contains("..")) {
            throw new BadRequestException("Invalid subfolder name");
        }
        return trimmed;
    }

    private String extensionOf(String originalName) {
        if (originalName == null) return "";
        int idx = originalName.lastIndexOf('.');
        if (idx < 0 || idx == originalName.length() - 1) return "";
        String ext = originalName.substring(idx + 1).toLowerCase(Locale.ROOT);
        return ext.matches("^[a-z0-9]{1,8}$") ? ext : "";
    }
}
