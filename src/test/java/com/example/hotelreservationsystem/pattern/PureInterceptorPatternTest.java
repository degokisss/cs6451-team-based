package com.example.hotelreservationsystem.pattern;

import com.example.hotelreservationsystem.pattern.interceptors.PureAuditInterceptor;
import com.example.hotelreservationsystem.pattern.interceptors.PureAuthenticationInterceptor;
import com.example.hotelreservationsystem.pattern.interceptors.PurePerformanceInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Interceptor Pattern Implementation.
 * <p>
 * This test demonstrates the framework-agnostic interceptor pattern
 * without relying on Spring's HandlerInterceptor integration.
 */
class PureInterceptorPatternTest {

    private PureAuthenticationInterceptor authInterceptor;
    private PureAuditInterceptor auditInterceptor;
    private PurePerformanceInterceptor perfInterceptor;
    private InterceptorManager interceptorManager;

    @BeforeEach
    void setUp() {
        authInterceptor = new PureAuthenticationInterceptor();
        auditInterceptor = new PureAuditInterceptor();
        perfInterceptor = new PurePerformanceInterceptor();
        interceptorManager = new InterceptorManager(
            perfInterceptor, auditInterceptor, authInterceptor
        );
    }

    @Test
    void shouldExecuteInterceptorChain() {
        // Given: HTTP request/response
        HttpServletRequest request = createMockRequest("/api/auth/login", "POST");
        HttpServletResponse response = new MockHttpServletResponse();
        RequestContext context = new RequestContext(request, response);

        // When: Create and execute chain
        InterceptorChain chain = interceptorManager.createAuthChain();

        boolean beforeResult = chain.executeBefore(context);

        // Simulate operation
        context.setStatusCode(200);

        chain.executeAfter(context);
        chain.executeAfterCompletion(context, null);

        // Then: All phases executed
        assertTrue(beforeResult, "Before phase should allow execution");
        assertEquals(200, context.getStatusCode());
    }

    @Test
    void shouldExecuteInterceptorsInOrder() {
        // Given: Chain with 3 interceptors
        InterceptorChain chain = interceptorManager.createAuthChain();

        // Then: Should have 3 interceptors
        assertEquals(3, chain.size());

        // And: Should be in correct order
        var names = chain.getInterceptorNames();
        assertEquals("PurePerformanceInterceptor", names.get(0), "Performance should be first (order=10)");
        assertEquals("PureAuditInterceptor", names.get(1), "Audit should be second (order=20)");
        assertEquals("PureAuthenticationInterceptor", names.get(2), "Auth should be third (order=30)");
    }

    @Test
    void shouldExecuteOperationWithChain() throws Exception {
        // Given: Request context
        HttpServletRequest request = createMockRequest("/api/auth/login", "POST");
        HttpServletResponse response = new MockHttpServletResponse();
        RequestContext context = new RequestContext(request, response);

        // When: Execute operation with chain
        InterceptorChain chain = interceptorManager.createAuthChain();

        String result = chain.executeChain(context, () -> {
            // Simulate business logic
            context.setStatusCode(200);
            return "Login successful";
        });

        // Then: Operation executed and interceptors ran
        assertEquals("Login successful", result);
        assertEquals(200, context.getStatusCode());
        assertTrue(context.getDurationMs() >= 0);
    }

    @Test
    void shouldStopChainIfInterceptorReturnsFalse() {
        // Given: Interceptor that blocks requests
        Interceptor blockingInterceptor = new Interceptor() {
            @Override
            public boolean before(RequestContext context) {
                return false; // Block execution
            }

            @Override
            public void after(RequestContext context) {}

            @Override
            public void afterCompletion(RequestContext context, Exception exception) {}

            @Override
            public int getOrder() {
                return 1;
            }
        };

        // When: Add blocking interceptor to chain
        InterceptorChain chain = new InterceptorChain();
        chain.addInterceptor(blockingInterceptor);

        HttpServletRequest request = createMockRequest("/api/auth/login", "POST");
        RequestContext context = new RequestContext(request, new MockHttpServletResponse());

        boolean result = chain.executeBefore(context);

        // Then: Chain should stop
        assertFalse(result, "Chain should be blocked");
    }

    @Test
    void shouldCaptureAuditEvents() {
        // Given: Request context
        HttpServletRequest request = createMockRequest("/api/auth/login", "POST");
        RequestContext context = new RequestContext(request, new MockHttpServletResponse());

        // When: Execute interceptors
        auditInterceptor.before(context);
        context.setStatusCode(200);
        auditInterceptor.after(context);
        auditInterceptor.afterCompletion(context, null);

        // Then: Audit event should be captured
        var events = auditInterceptor.getAuditLog();
        assertFalse(events.isEmpty(), "Should have audit events");

        var loginEvent = events.stream()
            .filter(e -> e.getEventType().equals("LOGIN_ATTEMPT"))
            .findFirst();

        assertTrue(loginEvent.isPresent(), "Should have login attempt event");
        assertEquals("SUCCESS", loginEvent.get().getStatus());
        assertEquals(200, loginEvent.get().getHttpStatusCode());
    }

    @Test
    void shouldTrackPerformanceMetrics() throws InterruptedException {
        // Given: Request context
        HttpServletRequest request = createMockRequest("/api/auth/login", "POST");
        RequestContext context = new RequestContext(request, new MockHttpServletResponse());

        // When: Execute with some delay
        perfInterceptor.before(context);
        Thread.sleep(50); // Simulate work
        context.setStatusCode(200);
        perfInterceptor.afterCompletion(context, null);

        // Then: Should track metrics
        var metrics = perfInterceptor.getAllMetrics();
        assertFalse(metrics.isEmpty(), "Should have performance metrics");

        var metric = metrics.getFirst();
        assertTrue(metric.getExecutionTimeMs() >= 50, "Should track execution time");
        assertEquals(200, metric.getStatusCode());
    }

    @Test
    void shouldHandleExceptions() {
        // Given: Operation that throws exception
        HttpServletRequest request = createMockRequest("/api/auth/login", "POST");
        RequestContext context = new RequestContext(request, new MockHttpServletResponse());

        InterceptorChain chain = interceptorManager.createAuthChain();

        // When: Execute operation that fails
        Exception thrown = assertThrows(Exception.class, () -> chain.executeChain(context, () -> {
            throw new RuntimeException("Operation failed");
        }));

        // Then: Exception should be caught and rethrown
        assertEquals("Operation failed", thrown.getMessage());
        // And: After completion should still run for cleanup
    }

    @Test
    void shouldCreateDifferentChainTypes() {
        // Test different chain configurations
        var authChain = interceptorManager.createAuthChain();
        assertEquals(3, authChain.size(), "Auth chain should have 3 interceptors");

        var minimalChain = interceptorManager.createMinimalChain();
        assertEquals(2, minimalChain.size(), "Minimal chain should have 2 interceptors");

        var perfChain = interceptorManager.createPerformanceChain();
        assertEquals(1, perfChain.size(), "Performance chain should have 1 interceptor");
    }

    @Test
    void shouldExtractClientIPFromContext() {
        // Given: Request with X-Forwarded-For header
        MockHttpServletRequest request = createMockRequest("/api/auth/login", "POST");
        request.addHeader("X-Forwarded-For", "192.168.1.100, 10.0.0.1");

        RequestContext context = new RequestContext(request, new MockHttpServletResponse());

        // When: Get client IP
        String clientIP = context.getClientIP();

        // Then: Should extract first IP from X-Forwarded-For
        assertEquals("192.168.1.100", clientIP);
    }

    @Test
    void shouldShareDataBetweenInterceptors() {
        // Given: Context with shared data
        HttpServletRequest request = createMockRequest("/api/auth/login", "POST");
        RequestContext context = new RequestContext(request, new MockHttpServletResponse());

        // When: Set attribute in one interceptor
        context.setAttribute("userId", 123L);
        context.setAttribute("userName", "testuser");

        // Then: Other interceptors can access it
        Long userId = context.getAttribute("userId");
        assertEquals(123L, userId);
        assertEquals("testuser", context.getAttribute("userName"));
    }

    /**
     * Helper to create mock HTTP request
     */
    private MockHttpServletRequest createMockRequest(String uri, String method) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        request.setMethod(method);
        request.setContentType("application/json");
        request.addHeader("User-Agent", "Test Agent");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
