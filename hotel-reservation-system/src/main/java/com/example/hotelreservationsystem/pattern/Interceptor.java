package com.example.hotelreservationsystem.pattern;

/**
 *
 * This is a framework-agnostic interceptor interface that implements
 * the classic Interceptor design pattern without any Spring dependencies.
 * <p>
 * The Interceptor pattern allows you to add preprocessing and postprocessing
 * to operations in a transparent, pluggable way.
 * <p>
 * Pattern Structure:
 * - before(): Called before the main operation (preprocessing)
 * - after(): Called after successful operation (postprocessing)
 * - afterCompletion(): Called after operation completes (cleanup)
 *
 * @see "Design Patterns: Elements of Reusable Object-Oriented Software"
 * @see "Java EE Core Patterns - Intercepting Filter"
 */
public interface Interceptor {

    /**
     * Called BEFORE the target operation executes.
     * <p>
     * Use this for:
     * - Request validation
     * - Logging incoming requests
     * - Setting up context
     * - Security checks
     *
     * @param context Request context containing request/response and attributes
     * @return true to continue execution, false to stop the chain
     */
    boolean before(RequestContext context);

    /**
     * Called AFTER the target operation executes successfully.
     * <p>
     * Use this for:
     * - Response modification
     * - Logging results
     * - Post-processing
     *
     * @param context Request context containing request/response and attributes
     */
    void after(RequestContext context);

    /**
     * Called AFTER the operation completes (success or failure).
     * Always called for cleanup, even if exceptions occurred.
     * <p>
     * Use this for:
     * - Resource cleanup
     * - Final logging
     * - Error handling
     *
     * @param context Request context
     * @param exception Exception that occurred (null if none)
     */
    void afterCompletion(RequestContext context, Exception exception);

    /**
     * Get interceptor name for identification and logging.
     *
     * @return Interceptor name
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Get execution order (lower numbers execute first).
     * Default is 100 (medium priority).
     * <p>
     * Suggested ranges:
     * - 1-50: High priority (logging, security)
     * - 51-100: Medium priority (validation)
     * - 101-200: Low priority (enhancement)
     *
     * @return Order value
     */
    default int getOrder() {
        return 100;
    }
}
