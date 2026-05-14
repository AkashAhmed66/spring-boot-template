package com.template.springboot.modules.permission.enums;

public final class PermissionName {

    private PermissionName() {}

    public static final String USER_READ        = "USER_READ";
    public static final String USER_WRITE       = "USER_WRITE";
    public static final String USER_DELETE      = "USER_DELETE";
    public static final String USER_IMPERSONATE = "USER_IMPERSONATE";

    public static final String ROLE_READ       = "ROLE_READ";
    public static final String ROLE_WRITE      = "ROLE_WRITE";
    public static final String ROLE_DELETE     = "ROLE_DELETE";

    public static final String PERMISSION_READ  = "PERMISSION_READ";
    public static final String PERMISSION_WRITE = "PERMISSION_WRITE";

    public static final String PRODUCT_READ   = "PRODUCT_READ";
    public static final String PRODUCT_WRITE  = "PRODUCT_WRITE";
    public static final String PRODUCT_DELETE = "PRODUCT_DELETE";

    public static final String AUDIT_READ = "AUDIT_READ";

    public static final String SESSION_READ   = "SESSION_READ";
    public static final String SESSION_REVOKE = "SESSION_REVOKE";

    /** Holding ANY of these bypasses per-record ownership checks in the service layer. */
    public static final String ADMIN_READ   = "ADMIN_READ";
    public static final String ADMIN_WRITE  = "ADMIN_WRITE";
    public static final String ADMIN_EDIT   = "ADMIN_EDIT";
    public static final String ADMIN_DELETE = "ADMIN_DELETE";

    public static final String[] ADMIN_ANY = { ADMIN_READ, ADMIN_WRITE, ADMIN_EDIT, ADMIN_DELETE };
}
