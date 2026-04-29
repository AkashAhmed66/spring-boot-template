package com.template.springboot.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {

    private boolean success;
    private String message;
    private Object data;
    private Object errors;
    private Instant timestamp;

    @JsonIgnore
    private HttpStatus status;

    public ApiResponse() {
        this.success = true;
        this.message = "OK";
        this.timestamp = Instant.now();
        this.status = HttpStatus.OK;
    }

    public ApiResponse(Object data) {
        this();
        this.data = unwrap(data);
    }

    public ApiResponse(Object data, String message) {
        this(data);
        this.message = message;
    }

    public ApiResponse(Object data, String message, HttpStatus status) {
        this(data, message);
        this.status = status;
    }

    public static ApiResponse created(Object data) {
        ApiResponse r = new ApiResponse(data, "Created");
        r.status = HttpStatus.CREATED;
        return r;
    }

    public static ApiResponse created(Object data, String message) {
        ApiResponse r = new ApiResponse(data, message);
        r.status = HttpStatus.CREATED;
        return r;
    }

    public static ApiResponse noContent() {
        ApiResponse r = new ApiResponse();
        r.status = HttpStatus.NO_CONTENT;
        return r;
    }

    public static ApiResponse error(String message, Object errors) {
        ApiResponse r = new ApiResponse();
        r.success = false;
        r.message = message;
        r.errors = errors;
        r.status = HttpStatus.BAD_REQUEST;
        return r;
    }

    public static ApiResponse error(String message, Object errors, HttpStatus status) {
        ApiResponse r = error(message, errors);
        r.status = status;
        return r;
    }

    public static ApiResponse message(String message) {
        ApiResponse r = new ApiResponse();
        r.message = message;
        return r;
    }

    private static Object unwrap(Object data) {
        if (data instanceof Page<?> page) {
            return PageResponse.of(page);
        }
        return data;
    }

    public boolean isSuccess()       { return success; }
    public String getMessage()       { return message; }
    public Object getData()          { return data; }
    public Object getErrors()        { return errors; }
    public Instant getTimestamp()    { return timestamp; }

    @JsonIgnore
    public HttpStatus getStatus()    { return status; }

    public ApiResponse setMessage(String message)    { this.message = message; return this; }
    public ApiResponse setSuccess(boolean success)   { this.success = success; return this; }
    public ApiResponse setStatus(HttpStatus status)  { this.status = status; return this; }
}
