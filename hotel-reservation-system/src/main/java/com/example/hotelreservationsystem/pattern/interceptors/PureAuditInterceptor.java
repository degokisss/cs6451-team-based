package com.example.hotelreservationsystem.pattern.interceptors;

import com.example.hotelreservationsystem.pattern.Interceptor;
import com.example.hotelreservationsystem.pattern.RequestContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure Audit Interceptor - Framework-agnostic audit logging.
 * <p>
 * Creates comprehensive audit trails for authentication events
 * without depending on Spring's HandlerInterceptor interface.
 * <p>
 * Responsibilities:
 * - Track all authentication attempts
 * - Record security-relevant data
 * - Generate audit events
 * - Store for compliance
 */
@Slf4j
@Component
public class PureAuditInterceptor implements Interceptor {

    private static final String EVENT_ID_KEY = "audit.eventId";
    private static final String START_TIME_KEY = "audit.startTime";

    // In-memory audit log
    private final List<AuditEvent> auditLog = Collections.synchronizedList(new ArrayList<>());

    @Override
    public boolean before(RequestContext context) {
        String uri = context.getRequestURI();

        // Only audit auth endpoints
        if (isNotAuthEndpoint(uri)) {
            return true;
        }

        String eventId = generateEventId();
        LocalDateTime startTime = LocalDateTime.now();

        context.setAttribute(EVENT_ID_KEY, eventId);
        context.setAttribute(START_TIME_KEY, startTime);

        // Create initial audit event
        AuditEvent event = AuditEvent.builder()
            .eventId(eventId)
            .timestamp(startTime)
            .eventType(determineEventType(uri, context.getMethod()))
            .requestURI(uri)
            .httpMethod(context.getMethod())
            .clientIP(context.getClientIP())
            .userAgent(context.getUserAgent())
            .status("INITIATED")
            .build();

        auditLog.add(event);

        log.info("📋 [PURE AUDIT] Event {} - {} {} initiated from {}",
            eventId, context.getMethod(), uri, event.getClientIP());

        return true;
    }

    @Override
    public void after(RequestContext context) {
        // After phase - can add additional processing here if needed
        log.debug("[PURE AUDIT] After phase for {}", context.getRequestURI());
    }

    @Override
    public void afterCompletion(RequestContext context, Exception exception) {
        String uri = context.getRequestURI();

        if (isNotAuthEndpoint(uri)) {
            return;
        }

        String eventId = context.getAttribute(EVENT_ID_KEY);
        LocalDateTime startTime = context.getAttribute(START_TIME_KEY);

        if (eventId == null) {
            return;
        }

        // Find and update audit event
        auditLog.stream()
            .filter(e -> e.getEventId().equals(eventId))
            .findFirst()
            .ifPresent(event -> {
                event.setCompletedAt(LocalDateTime.now());
                event.setHttpStatusCode(context.getStatusCode());
                event.setStatus(determineStatus(context.getStatusCode(), exception));

                if (exception != null) {
                    event.setErrorMessage(exception.getMessage());
                }

                if (startTime != null) {
                    event.setDurationMs(
                        java.time.Duration.between(startTime, LocalDateTime.now()).toMillis()
                    );
                }

                logAuditResult(event);
            });

        // Cleanup
        context.removeAttribute(EVENT_ID_KEY);
        context.removeAttribute(START_TIME_KEY);
    }

    private boolean isNotAuthEndpoint(String uri) {
        return !uri.contains("/api/auth/") && !uri.contains("/oauth2/");
    }

    private String determineEventType(String uri, String method) {
        if (uri.contains("/login")) return "LOGIN_ATTEMPT";
        if (uri.contains("/logout")) return "LOGOUT_REQUEST";
        if (uri.contains("/register")) return "REGISTRATION_ATTEMPT";
        if (uri.contains("/oauth2/")) return "OAUTH2_AUTHENTICATION";
        return "AUTH_REQUEST";
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
            "📋 [PURE AUDIT] Event %s - %s: %s (Status: %d, Duration: %dms, IP: %s)",
            event.getEventId(), event.getEventType(), event.getStatus(),
            event.getHttpStatusCode(), event.getDurationMs(), event.getClientIP()
        );

        switch (event.getStatus()) {
            case "SUCCESS" -> log.info(message);
            case "UNAUTHORIZED" -> log.warn(message + " - Auth failed");
            case "ERROR", "SERVER_ERROR" -> log.error(message + " - Error: {}", event.getErrorMessage());
            default -> log.warn(message);
        }
    }

    private String generateEventId() {
        return "AUDIT-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    public List<AuditEvent> getAuditLog() {
        return new ArrayList<>(auditLog);
    }

    public void clearAuditLog() {
        auditLog.clear();
    }

    @Override
    public int getOrder() {
        return 20; // Execute second (after Performance)
    }

    @Override
    public String getName() {
        return "PureAuditInterceptor";
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class AuditEvent {
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
