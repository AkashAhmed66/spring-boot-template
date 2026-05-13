package com.template.springboot.common.idempotency;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.idempotency")
public class IdempotencyProperties {

    private boolean enabled = true;

    private Duration ttl = Duration.ofHours(24);

    private Duration cleanupInterval = Duration.ofHours(1);
}
