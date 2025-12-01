package com.example.hotelreservationsystem.pattern;

import com.example.hotelreservationsystem.pattern.interceptors.AuthenticationInterceptor;
import com.example.hotelreservationsystem.pattern.interceptors.AuditInterceptor;
import com.example.hotelreservationsystem.pattern.interceptors.PerformanceInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterceptorManager {

    private final PerformanceInterceptor performanceInterceptor;
    private final AuditInterceptor auditInterceptor;
    private final AuthenticationInterceptor authenticationInterceptor;

    /**
     * Create interceptor chain for authentication endpoints.
     * Includes all three interceptors in proper order.
     * <p>
     * Execution order:
     * 1. Performance (order=10) - outermost
     * 2. Audit (order=20) - middle
     * 3. Authentication (order=30) - innermost
     *
     * @return Configured interceptor chain
     */
    public InterceptorChain createAuthChain() {
        InterceptorChain chain = new InterceptorChain();

        // Add in any order - chain will sort by getOrder()
        chain.addInterceptor(performanceInterceptor);
        chain.addInterceptor(auditInterceptor);
        chain.addInterceptor(authenticationInterceptor);

        log.debug("Created auth interceptor chain with {} interceptors: {}",
            chain.size(), chain.getInterceptorNames());

        return chain;
    }

    /**
     * Create minimal chain (performance + audit only)
     * Useful for non-authentication endpoints
     */
    public InterceptorChain createMinimalChain() {
        InterceptorChain chain = new InterceptorChain();
        chain.addInterceptor(performanceInterceptor);
        chain.addInterceptor(auditInterceptor);

        log.debug("Created minimal interceptor chain with {} interceptors", chain.size());
        return chain;
    }

    /**
     * Create performance-only chain
     * Useful for high-throughput endpoints
     */
    public InterceptorChain createPerformanceChain() {
        InterceptorChain chain = new InterceptorChain();
        chain.addInterceptor(performanceInterceptor);

        log.debug("Created performance-only chain");
        return chain;
    }

    /**
     * Create custom chain with specific interceptors
     */
    public InterceptorChain createCustomChain(Interceptor... interceptors) {
        InterceptorChain chain = new InterceptorChain();
        for (Interceptor interceptor : interceptors) {
            chain.addInterceptor(interceptor);
        }

        log.debug("Created custom chain with {} interceptors", chain.size());
        return chain;
    }
}
