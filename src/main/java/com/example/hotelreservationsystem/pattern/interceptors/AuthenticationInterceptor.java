package com.example.hotelreservationsystem.pattern.interceptors;

import com.example.hotelreservationsystem.pattern.Interceptor;
import com.example.hotelreservationsystem.pattern.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 *Authentication Interceptor.
 * <p>
 * Responsibilities:
 * - Validate authentication request format
 * - Log authentication attempts
 * - Track authentication results
 * - Measure execution time
 */
@Slf4j
@Component
public class AuthenticationInterceptor implements Interceptor {

    private static final String START_TIME_KEY = "auth.startTime";

    @Override
    public boolean before(RequestContext context) {
        String uri = context.getRequestURI();
        String method = context.getMethod();

        // Store start time
        context.setAttribute(START_TIME_KEY, System.currentTimeMillis());

        log.info("[PURE AUTH] BEFORE - {} {} from IP: {}",
            method, uri, context.getClientIP());

        // Validate based on endpoint
        if (uri.contains("/login")) {
            return validateLogin(context);
        } else if (uri.contains("/logout")) {
            return validateLogout(context);
        } else if (uri.contains("/register")) {
            log.info("[PURE AUTH] Registration request detected");
        }

        return true; // Continue
    }

    @Override
    public void after(RequestContext context) {
        int status = context.getStatusCode();
        String uri = context.getRequestURI();

        log.info("[PURE AUTH] AFTER - {} completed with status: {}", uri, status);

        // Log result
        if (status >= 200 && status < 300) {
            log.info(" [PURE AUTH] Authentication successful");
        } else if (status == 401) {
            log.warn(" [PURE AUTH] Authentication failed - Unauthorized");
        } else if (status >= 400) {
            log.warn(" [PURE AUTH] Authentication failed - Status: {}", status);
        }
    }

    @Override
    public void afterCompletion(RequestContext context, Exception exception) {
        Long startTime = context.getAttribute(START_TIME_KEY);

        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            log.info("[PURE AUTH] COMPLETE - {} - Total time: {}ms",
                context.getRequestURI(), duration);
        }

        if (exception != null) {
            log.error("[PURE AUTH] Exception occurred: {}", exception.getMessage(), exception);
        }

        // Cleanup
        context.removeAttribute(START_TIME_KEY);
    }

    /**
     * Validate login request
     */
    private boolean validateLogin(RequestContext context) {
        String contentType = context.getContentType();

        if (contentType == null || !contentType.contains("application/json")) {
            log.warn("[PURE AUTH] Invalid login - Content-Type should be application/json");
            // Allow to continue - controller will handle
        }

        log.info("[PURE AUTH] Login attempt from: {}", context.getClientIP());
        return true;
    }

    /**
     * Validate logout request
     */
    private boolean validateLogout(RequestContext context) {
        String authHeader = context.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[PURE AUTH] Logout without valid Authorization header");
            // Allow to continue - controller will handle
        } else {
            log.info("[PURE AUTH] Logout from: {}", context.getClientIP());
        }

        return true;
    }

    @Override
    public int getOrder() {
        return 30; // Execute third (after Performance and Audit)
    }

    @Override
    public String getName() {
        return "PureAuthenticationInterceptor";
    }
}
