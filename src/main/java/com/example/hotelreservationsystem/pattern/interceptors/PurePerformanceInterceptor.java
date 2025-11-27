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
 * Performance Interceptor - Framework-agnostic performance monitoring.
 * <p>
 * Monitors response times and performance metrics
 * <p>
 * Responsibilities:
 * - Track execution time
 * - Identify slow requests
 * - Calculate performance statistics
 * - Alert on performance issues
 */
@Slf4j
@Component
public class PurePerformanceInterceptor implements Interceptor {

    private static final long SLOW_THRESHOLD = 1000; // 1 second
    private static final long WARNING_THRESHOLD = 500; // 500ms
    private static final String START_TIME_KEY = "perf.startTime";

    private final List<PerformanceMetric> metrics = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_METRICS = 1000;

    @Override
    public boolean before(RequestContext context) {
        context.setAttribute(START_TIME_KEY, System.currentTimeMillis());
        log.debug("[PURE PERF] Starting timer for {}", context.getRequestURI());
        return true;
    }

    @Override
    public void after(RequestContext context) {
        // After phase - can add processing here if needed
    }

    @Override
    public void afterCompletion(RequestContext context, Exception exception) {
        Long startTime = context.getAttribute(START_TIME_KEY);
        if (startTime == null) {
            return;
        }

        long executionTime = System.currentTimeMillis() - startTime;
        int statusCode = context.getStatusCode();

        // Create metric
        PerformanceMetric metric = PerformanceMetric.builder()
            .timestamp(LocalDateTime.now())
            .requestURI(context.getRequestURI())
            .httpMethod(context.getMethod())
            .executionTimeMs(executionTime)
            .statusCode(statusCode)
            .success(statusCode >= 200 && statusCode < 300)
            .clientIP(context.getClientIP())
            .build();

        addMetric(metric);
        logPerformance(metric);

        // Check for performance issues
        if (executionTime > SLOW_THRESHOLD) {
            log.warn("🐌 [PURE PERF] SLOW REQUEST: {} {} - {}ms (threshold: {}ms)",
                context.getMethod(), context.getRequestURI(), executionTime, SLOW_THRESHOLD);
        } else if (executionTime > WARNING_THRESHOLD) {
            log.warn("⚠️ [PURE PERF] Approaching threshold: {} {} - {}ms",
                context.getMethod(), context.getRequestURI(), executionTime);
        }

        context.removeAttribute(START_TIME_KEY);
    }

    private void addMetric(PerformanceMetric metric) {
        metrics.add(metric);
        if (metrics.size() > MAX_METRICS) {
            metrics.removeFirst();
        }
    }

    private void logPerformance(PerformanceMetric metric) {
        String emoji = getPerformanceEmoji(metric.getExecutionTimeMs());
        log.info("{} [PURE PERF] {} {} - {}ms - Status: {}",
            emoji, metric.getHttpMethod(), metric.getRequestURI(),
            metric.getExecutionTimeMs(), metric.getStatusCode());
    }

    private String getPerformanceEmoji(long time) {
        if (time < 100) return "⚡";
        if (time < WARNING_THRESHOLD) return "🚀";
        if (time < SLOW_THRESHOLD) return "🐢";
        return "🐌";
    }

    public List<PerformanceMetric> getAllMetrics() {
        return new ArrayList<>(metrics);
    }

    public void clearMetrics() {
        metrics.clear();
    }

    @Override
    public int getOrder() {
        return 10; // Execute first (outermost layer)
    }

    @Override
    public String getName() {
        return "PurePerformanceInterceptor";
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class PerformanceMetric {
        private LocalDateTime timestamp;
        private String requestURI;
        private String httpMethod;
        private long executionTimeMs;
        private int statusCode;
        private boolean success;
        private String clientIP;
    }
}
