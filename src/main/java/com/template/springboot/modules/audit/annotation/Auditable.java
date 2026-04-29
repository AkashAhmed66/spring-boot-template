package com.template.springboot.modules.audit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds richer metadata to an audit-log entry for a controller method.
 * The HTTP-level capture happens unconditionally via the audit filter; this annotation
 * lets you label an action ("USER_CREATE") and bind it to a specific resource.
 *
 * SpEL is supported in {@link #resourceId()} with arguments exposed by name (e.g. "#id").
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** Domain action label e.g. "USER_CREATE", "PRODUCT_DELETE". */
    String action() default "";

    /** Resource type e.g. "User", "Product". */
    String resourceType() default "";

    /** SpEL expression resolved against method args, e.g. "#id" or "#request.email". */
    String resourceId() default "";

    /** Skip auditing this handler entirely. */
    boolean skip() default false;
}
