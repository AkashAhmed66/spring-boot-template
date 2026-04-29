package com.template.springboot.modules.permission.enums;

public final class PermissionName {

    private PermissionName() {}

    public static final String USER_READ   = "USER_READ";
    public static final String USER_WRITE  = "USER_WRITE";
    public static final String USER_DELETE = "USER_DELETE";

    public static final String ROLE_READ       = "ROLE_READ";
    public static final String ROLE_WRITE      = "ROLE_WRITE";
    public static final String ROLE_DELETE     = "ROLE_DELETE";

    public static final String PERMISSION_READ  = "PERMISSION_READ";
    public static final String PERMISSION_WRITE = "PERMISSION_WRITE";

    public static final String PRODUCT_READ   = "PRODUCT_READ";
    public static final String PRODUCT_WRITE  = "PRODUCT_WRITE";
    public static final String PRODUCT_DELETE = "PRODUCT_DELETE";

    public static final String AUDIT_READ = "AUDIT_READ";
}
