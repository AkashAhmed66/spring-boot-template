package com.template.springboot.modules.audit.context;

public final class AuditContextHolder {

    private static final ThreadLocal<AuditContext> HOLDER = new ThreadLocal<>();

    private AuditContextHolder() {}

    public static AuditContext getOrCreate() {
        AuditContext ctx = HOLDER.get();
        if (ctx == null) {
            ctx = new AuditContext();
            HOLDER.set(ctx);
        }
        return ctx;
    }

    public static AuditContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
