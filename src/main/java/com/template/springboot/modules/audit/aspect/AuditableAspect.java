package com.template.springboot.modules.audit.aspect;

import com.template.springboot.modules.audit.annotation.Auditable;
import com.template.springboot.modules.audit.context.AuditContext;
import com.template.springboot.modules.audit.context.AuditContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditableAspect {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    @Before("@annotation(com.template.springboot.modules.audit.annotation.Auditable)")
    public void capture(JoinPoint jp) {
        MethodSignature sig = (MethodSignature) jp.getSignature();
        Method method = sig.getMethod();
        Auditable ann = method.getAnnotation(Auditable.class);
        if (ann == null) return;

        AuditContext ctx = AuditContextHolder.getOrCreate();
        if (ann.skip()) {
            ctx.setSkip(true);
            return;
        }

        String resourceId = resolveResourceId(ann.resourceId(), method, jp.getArgs());
        ctx.merge(ann.action(), ann.resourceType(), resourceId);
    }

    private static String resolveResourceId(String spel, Method method, Object[] args) {
        if (spel == null || spel.isBlank()) return null;
        try {
            EvaluationContext ec = new StandardEvaluationContext();
            Parameter[] params = method.getParameters();
            for (int i = 0; i < params.length; i++) {
                ec.setVariable(params[i].getName(), args[i]);
            }
            Expression expr = PARSER.parseExpression(spel);
            Object value = expr.getValue(ec);
            return value == null ? null : value.toString();
        } catch (Exception ex) {
            log.debug("Audit resourceId SpEL '{}' failed: {}", spel, ex.getMessage());
            return null;
        }
    }
}
