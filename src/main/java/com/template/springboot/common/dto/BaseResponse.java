package com.template.springboot.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Mirror of {@link com.template.springboot.common.audit.BaseEntity}'s audit + soft-delete fields,
 * used as the parent of every entity-backed response DTO so these fields never have to be redeclared.
 *
 * <p>Field names match the entity exactly so ModelMapper populates them automatically through the
 * inherited Lombok setters — adding a new audit/lifecycle field needs only a single edit here.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseResponse {

    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
    private Instant deletedAt;
    private String deletedBy;
}
