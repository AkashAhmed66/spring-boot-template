package com.template.springboot.common.exception;

import com.template.springboot.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ApiResponse handleNotFound(ResourceNotFoundException ex) {
        return ApiResponse.error(ex.getMessage(), null, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ApiResponse handleBadRequest(BadRequestException ex) {
        return ApiResponse.error(ex.getMessage(), null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ApiResponse handleDuplicate(DuplicateResourceException ex) {
        return ApiResponse.error(ex.getMessage(), null, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ApiResponse.error("Validation failed", fieldErrors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse handleMissingParam(MissingServletRequestParameterException ex) {
        return ApiResponse.error("Missing parameter: " + ex.getParameterName(), null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ApiResponse.error("Invalid value for parameter '%s'".formatted(ex.getName()), null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResponse handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ApiResponse.error(ex.getMessage(), null, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ApiResponse handleBadCredentials(BadCredentialsException ex) {
        return ApiResponse.error("Invalid credentials", null, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ApiResponse handleAuth(AuthenticationException ex) {
        return ApiResponse.error(ex.getMessage(), null, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse handleAccessDenied(AccessDeniedException ex) {
        return ApiResponse.error("Access denied", null, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        return ApiResponse.error("Internal server error", null, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
