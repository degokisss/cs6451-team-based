package com.example.hotelreservationsystem.aop;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Spring AOP-based interceptor to mirror the custom interceptor behavior for room endpoints.
 * Use {@link BuiltInInterceptor} on controller methods to enable it.
 */
@Aspect
@Component
@Slf4j
public class BuiltInAuthInterceptor {

    private static final long SLOW_THRESHOLD = 1000; // 1 second
    private static final long WARNING_THRESHOLD = 500; // 500ms

    private final List<PerformanceMetric> metrics = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_METRICS = 1000;

    private final List<AuditEvent> auditLog = Collections.synchronizedList(new ArrayList<>());

    @Around("@annotation(com.example.hotelreservationsystem.aop.BuiltInInterceptor)"
        + " || @within(com.example.hotelreservationsystem.aop.BuiltInInterceptor)")
    public Object aroundAuthEndpoints(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        HttpServletRequest request = currentRequest();
        HttpServletResponse response = currentResponse();

        String uri = request != null ? request.getRequestURI() : "N/A";
        String method = request != null ? request.getMethod() : "N/A";
        String clientIp = request != null ? clientIp(request) : "unknown";

        AuditEvent auditEvent = beginAudit(uri, method, clientIp, request);

        log.info("[AOP AUTH] BEFORE - {} {} from IP: {}", method, uri, clientIp);
        validate(request, uri);

        try {
            Object result = pjp.proceed();

            int status = response != null ? response.getStatus() : 200;
            log.info("[AOP AUTH] AFTER - {} completed with status: {}", uri, status);
            completeAudit(auditEvent, status, null);
            recordPerformance(uri, method, clientIp, status, start);
            return result;
        } catch (Exception ex) {
            log.error("[AOP AUTH] Exception during {} {}: {}", method, uri, ex.getMessage(), ex);
            if (response != null && response.getStatus() == 0) {
                response.setStatus(500);
            }
            completeAudit(auditEvent, response != null ? response.getStatus() : 500, ex);
            recordPerformance(uri, method, clientIp, response != null ? response.getStatus() : 500, start);
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("[AOP AUTH] COMPLETE - {} {} took {}ms", method, uri, duration);
        }
    }

    private void validate(HttpServletRequest request, String uri) {
        // No-op validation for room endpoints; hook left for future checks if needed.
    }

    private AuditEvent beginAudit(String uri, String method, String clientIp, HttpServletRequest request) {
        if (!isRoomsEndpoint(uri)) {
            return null;
        }

        AuditEvent event = AuditEvent.builder()
            .eventId(generateEventId())
            .timestamp(LocalDateTime.now())
            .eventType(determineEventType(uri, method))
            .requestURI(uri)
            .httpMethod(method)
            .clientIP(clientIp)
            .userAgent(request != null ? request.getHeader("User-Agent") : null)
            .status("INITIATED")
            .build();

        auditLog.add(event);
        log.info("[AOP AUDIT] Event {} - {} {} initiated from {}", event.getEventId(), method, uri, clientIp);
        return event;
    }

    private void completeAudit(AuditEvent event, int statusCode, Exception exception) {
        if (event == null) {
            return;
        }

        event.setCompletedAt(LocalDateTime.now());
        event.setHttpStatusCode(statusCode);
        event.setStatus(determineStatus(statusCode, exception));
        event.setDurationMs(
            java.time.Duration.between(event.getTimestamp(), event.getCompletedAt()).toMillis()
        );

        if (exception != null) {
            event.setErrorMessage(exception.getMessage());
        }

        logAuditResult(event);
    }

    private void recordPerformance(String uri, String method, String clientIp, int statusCode, long start) {
        long executionTime = System.currentTimeMillis() - start;

        PerformanceMetric metric = PerformanceMetric.builder()
            .timestamp(LocalDateTime.now())
            .requestURI(uri)
            .httpMethod(method)
            .executionTimeMs(executionTime)
            .statusCode(statusCode)
            .success(statusCode >= 200 && statusCode < 300)
            .clientIP(clientIp)
            .build();

        metrics.add(metric);
        if (metrics.size() > MAX_METRICS) {
            metrics.remove(0);
        }

        logPerformance(metric);
    }

    private void logPerformance(PerformanceMetric metric) {
        String emoji = getPerformanceEmoji(metric.getExecutionTimeMs());
        log.info("{} [AOP PERF] {} {} - {}ms - Status: {}",
            emoji, metric.getHttpMethod(), metric.getRequestURI(),
            metric.getExecutionTimeMs(), metric.getStatusCode());

        if (metric.getExecutionTimeMs() > SLOW_THRESHOLD) {
            log.warn("[AOP PERF] SLOW REQUEST: {} {} - {}ms (threshold: {}ms)",
                metric.getHttpMethod(), metric.getRequestURI(), metric.getExecutionTimeMs(), SLOW_THRESHOLD);
        } else if (metric.getExecutionTimeMs() > WARNING_THRESHOLD) {
            log.warn("[AOP PERF] Approaching threshold: {} {} - {}ms",
                metric.getHttpMethod(), metric.getRequestURI(), metric.getExecutionTimeMs());
        }
    }

    private String getPerformanceEmoji(long time) {
        if (time < 100) return "⚡";
        if (time < WARNING_THRESHOLD) return "🚀";
        if (time < SLOW_THRESHOLD) return "🐢";
        return "";
    }

    private boolean isRoomsEndpoint(String uri) {
        return uri != null && uri.contains("/api/rooms");
    }

    private String determineEventType(String uri, String method) {
        return "ROOM_REQUEST_" + method;
    }

    private String determineStatus(int statusCode, Exception ex) {
        if (ex != null) return "ERROR";
        if (statusCode >= 200 && statusCode < 300) return "SUCCESS";
        if (statusCode == 401) return "UNAUTHORIZED";
        if (statusCode == 400) return "INVALID_REQUEST";
        if (statusCode >= 500) return "SERVER_ERROR";
        return "FAILED";
    }

    private void logAuditResult(AuditEvent event) {
        String message = String.format(
            "[AOP AUDIT] Event %s - %s: %s (Status: %d, Duration: %dms, IP: %s)",
            event.getEventId(), event.getEventType(), event.getStatus(),
            event.getHttpStatusCode(), event.getDurationMs(), event.getClientIP()
        );

        switch (event.getStatus()) {
            case "SUCCESS" -> log.info(message);
            case "UNAUTHORIZED" -> log.warn("{} - Auth failed", message);
            case "ERROR", "SERVER_ERROR" -> log.error("{} - Error: {}", message, event.getErrorMessage());
            default -> log.warn(message);
        }
    }

    private String generateEventId() {
        return "AOP-AUDIT-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }

    private HttpServletResponse currentResponse() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getResponse();
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
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

    @Data
    @Builder
    @AllArgsConstructor
    private static class PerformanceMetric {
        private LocalDateTime timestamp;
        private String requestURI;
        private String httpMethod;
        private long executionTimeMs;
        private int statusCode;
        private boolean success;
        private String clientIP;
    }

    @Data
    @Builder
    @AllArgsConstructor
    private static class AuditEvent {
        private String eventId;
        private LocalDateTime timestamp;
        private LocalDateTime completedAt;
        private String eventType;
        private String requestURI;
        private String httpMethod;
        private String clientIP;
        private String userAgent;
        private String status;
        private Integer httpStatusCode;
        private Long durationMs;
        private String errorMessage;
    }
}
