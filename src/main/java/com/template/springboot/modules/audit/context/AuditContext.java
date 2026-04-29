package com.template.springboot.modules.audit.context;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditContext {

    private String action;
    private String resourceType;
    private String resourceId;
    private boolean skip;

    public void merge(String action, String resourceType, String resourceId) {
        if (action != null && !action.isBlank()) this.action = action;
        if (resourceType != null && !resourceType.isBlank()) this.resourceType = resourceType;
        if (resourceId != null && !resourceId.isBlank()) this.resourceId = resourceId;
    }
}
