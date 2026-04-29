package com.template.springboot.modules.audit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.audit")
public class AuditProperties {

    /** Master switch — when false, no requests are captured and the read API is disabled. */
    private boolean enabled = true;

    /** Controls exposure of the GET /api/v1/audit-logs endpoint independently of capture. */
    private boolean exposeApi = true;

    /** When false, only annotated handlers are audited. When true, every API call is audited. */
    private boolean auditAll = true;

    /** Persist the request body. */
    private boolean captureRequestBody = true;

    /** Persist the response body. */
    private boolean captureResponseBody = true;

    /** Max characters to retain for request/response bodies before truncation. */
    private int maxBodyLength = 10_000;

    /** Path prefixes to audit. Default = "/api/". */
    private List<String> includePathPrefixes = List.of("/api/");

    /** Paths skipped entirely (regex contains-match against the URI). */
    private List<String> excludePathPatterns = List.of(
            "/actuator/.*",
            ".*/swagger-ui.*",
            "/v3/api-docs.*",
            "/api/v1/files/.*"
    );

    /** Field names whose values are masked in captured bodies. */
    private List<String> maskedFields = List.of(
            "password", "passwordHash", "currentPassword", "newPassword",
            "token", "accessToken", "refreshToken", "secret", "apiKey", "authorization"
    );
}
