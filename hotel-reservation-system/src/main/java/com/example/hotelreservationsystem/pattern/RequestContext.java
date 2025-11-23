package com.example.hotelreservationsystem.pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Request Context - Encapsulates request/response and shared data.
 * <p>
 * This class wraps the HTTP request and response, providing a clean API
 * for interceptors to access and modify request data without tight coupling
 * to servlet APIs.
 */
@Getter
public class RequestContext {

    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final Map<String, Object> attributes;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;

    /**
     * Create a new request context.
     *
     * @param request HTTP servlet request
     * @param response HTTP servlet response
     */
    public RequestContext(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
        this.attributes = new HashMap<>();
        this.startTime = LocalDateTime.now();
    }

    /**
     * Get request URI
     */
    public String getRequestURI() {
        return request.getRequestURI();
    }

    /**
     * Get HTTP method (GET, POST, etc.)
     */
    public String getMethod() {
        return request.getMethod();
    }

    /**
     * Get client IP address (handles proxy headers)
     */
    public String getClientIP() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * Get request header
     */
    public String getHeader(String name) {
        return request.getHeader(name);
    }

    /**
     * Get Content-Type header
     */
    public String getContentType() {
        return request.getContentType();
    }

    /**
     * Get User-Agent header
     */
    public String getUserAgent() {
        return request.getHeader("User-Agent");
    }

    /**
     * Get response status code
     */
    public int getStatusCode() {
        return response.getStatus();
    }

    /**
     * Set response status code
     */
    public void setStatusCode(int status) {
        response.setStatus(status);
    }

    /**
     * Set attribute (shared data between interceptors)
     */
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    /**
     * Get attribute
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name) {
        return (T) attributes.get(name);
    }

    /**
     * Get attribute with default value
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name, T defaultValue) {
        return (T) attributes.getOrDefault(name, defaultValue);
    }

    /**
     * Check if attribute exists
     */
    public boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }

    /**
     * Remove attribute
     */
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    /**
     * Mark request as completed
     */
    public void markComplete() {
        this.endTime = LocalDateTime.now();
    }

    /**
     * Get execution duration in milliseconds
     */
    public long getDurationMs() {
        return Duration.between(startTime, Objects.requireNonNullElseGet(endTime, LocalDateTime::now)).toMillis();
    }

    /**
     * Check if request was successful (2xx status)
     */
    public boolean isSuccess() {
        int status = getStatusCode();
        return status >= 200 && status < 300;
    }

    /**
     * Check if request failed (4xx or 5xx status)
     */
    public boolean isError() {
        int status = getStatusCode();
        return status >= 400;
    }

    /**
     * Get debug information as string
     */
    @Override
    public String toString() {
        return String.format("RequestContext{method=%s, uri=%s, status=%d, duration=%dms, ip=%s}",
            getMethod(), getRequestURI(), getStatusCode(), getDurationMs(), getClientIP());
    }
}
