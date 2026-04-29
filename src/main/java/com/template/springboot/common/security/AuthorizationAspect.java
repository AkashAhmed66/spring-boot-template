package com.template.springboot.common.security;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Aspect
@Component
public class AuthorizationAspect {

    @Before("@annotation(com.template.springboot.common.security.HasRole) "
            + "|| @within(com.template.springboot.common.security.HasRole)")
    public void enforceRole(JoinPoint jp) {
        HasRole ann = resolve(jp, HasRole.class);
        if (ann == null) return;
        requireAuthenticated();
        if (!SecurityUtils.hasRole(ann.value())) {
            throw new AccessDeniedException("Required role: " + ann.value());
        }
    }

    @Before("@annotation(com.template.springboot.common.security.HasPermission) "
            + "|| @within(com.template.springboot.common.security.HasPermission)")
    public void enforcePermission(JoinPoint jp) {
        HasPermission ann = resolve(jp, HasPermission.class);
        if (ann == null) return;
        requireAuthenticated();
        if (!SecurityUtils.hasAuthority(ann.value())) {
            throw new AccessDeniedException("Required permission: " + ann.value());
        }
    }

    private static <A extends Annotation> A resolve(JoinPoint jp, Class<A> type) {
        MethodSignature sig = (MethodSignature) jp.getSignature();
        Method method = sig.getMethod();
        A ann = method.getAnnotation(type);
        if (ann == null) ann = method.getDeclaringClass().getAnnotation(type);
        return ann;
    }

    private static void requireAuthenticated() {
        if (SecurityUtils.getCurrentAuthentication().isEmpty()) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
    }
}
