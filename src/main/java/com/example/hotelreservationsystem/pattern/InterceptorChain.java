package com.example.hotelreservationsystem.pattern;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Interceptor Chain - Manages and executes multiple interceptors.
 * <p>
 * This class implements the Chain of Responsibility pattern combined with
 * the Interceptor pattern. It maintains an ordered list of interceptors
 * and ensures they execute in the correct sequence.
 * <p>
 * Execution Flow:
 * 1. before() - Execute all interceptors in order until one returns false
 * 2. Target operation (controller method)
 * 3. after() - Execute all interceptors in reverse order
 * 4. afterCompletion() - Always execute for cleanup (even if error)
 */
@Slf4j
public class InterceptorChain {

    private final List<Interceptor> interceptors;
    private final List<Interceptor> executedInterceptors;

    /**
     * Create empty interceptor chain
     */
    public InterceptorChain() {
        this.interceptors = new ArrayList<>();
        this.executedInterceptors = new ArrayList<>();
    }

    /**
     * Create chain with initial interceptors
     */
    public InterceptorChain(List<Interceptor> interceptors) {
        this.interceptors = new ArrayList<>(interceptors);
        this.executedInterceptors = new ArrayList<>();
        sortByOrder();
    }

    /**
     * Add interceptor to chain
     */
    public void addInterceptor(Interceptor interceptor) {
        interceptors.add(interceptor);
        sortByOrder();
        log.debug("Added interceptor: {} (order: {})", interceptor.getName(), interceptor.getOrder());
    }

    /**
     * Remove interceptor from chain
     */
    public void removeInterceptor(Interceptor interceptor) {
        interceptors.remove(interceptor);
        log.debug("Removed interceptor: {}", interceptor.getName());
    }

    /**
     * Execute BEFORE phase of all interceptors.
     * Stops execution if any interceptor returns false.
     *
     * @param context Request context
     * @return true if all interceptors passed, false if any blocked
     */
    public boolean executeBefore(RequestContext context) {
        log.debug("Executing BEFORE phase for {} interceptors", interceptors.size());

        for (Interceptor interceptor : interceptors) {
            try {
                log.trace("Executing before() on: {}", interceptor.getName());
                boolean shouldContinue = interceptor.before(context);

                // Track which interceptors executed successfully
                executedInterceptors.add(interceptor);

                if (!shouldContinue) {
                    log.warn("Interceptor {} returned false - stopping chain", interceptor.getName());
                    return false;
                }

            } catch (Exception ex) {
                log.error("Exception in interceptor {}.before(): {}",
                    interceptor.getName(), ex.getMessage(), ex);

                // Add to executed list even if failed (so afterCompletion runs)
                executedInterceptors.add(interceptor);

                // Optionally continue or stop on exception
                // Here we continue to next interceptor
            }
        }

        log.debug("BEFORE phase completed successfully");
        return true;
    }

    /**
     * Execute AFTER phase of all interceptors in REVERSE order.
     * Only called if operation succeeded.
     *
     * @param context Request context
     */
    public void executeAfter(RequestContext context) {
        log.debug("Executing AFTER phase for {} interceptors", executedInterceptors.size());

        // Execute in reverse order (LIFO - Last In First Out)
        for (int i = executedInterceptors.size() - 1; i >= 0; i--) {
            Interceptor interceptor = executedInterceptors.get(i);

            try {
                log.trace("Executing after() on: {}", interceptor.getName());
                interceptor.after(context);

            } catch (Exception ex) {
                log.error("Exception in interceptor {}.after(): {}",
                    interceptor.getName(), ex.getMessage(), ex);
                // Continue to next interceptor even if this one fails
            }
        }

        log.debug("AFTER phase completed");
    }

    /**
     * Execute AFTER COMPLETION phase for cleanup.
     * Always called, even if operation or other phases failed.
     *
     * @param context Request context
     * @param exception Exception that occurred (null if none)
     */
    public void executeAfterCompletion(RequestContext context, Exception exception) {
        log.debug("Executing AFTER COMPLETION phase for {} interceptors",
            executedInterceptors.size());

        if (exception != null) {
            log.debug("Executing with exception: {}", exception.getMessage());
        }

        // Execute in reverse order (LIFO)
        for (int i = executedInterceptors.size() - 1; i >= 0; i--) {
            Interceptor interceptor = executedInterceptors.get(i);

            try {
                log.trace("Executing afterCompletion() on: {}", interceptor.getName());
                interceptor.afterCompletion(context, exception);

            } catch (Exception ex) {
                log.error("Exception in interceptor {}.afterCompletion(): {}",
                    interceptor.getName(), ex.getMessage(), ex);
                // Continue cleanup even if this interceptor fails
            }
        }

        // Clear executed list for next request
        executedInterceptors.clear();

        log.debug("AFTER COMPLETION phase completed");
    }

    /**
     * Execute complete interceptor chain around an operation.
     * This is a convenience method that handles all phases.
     *
     * @param context Request context
     * @param operation The operation to execute (controller method)
     * @return Operation result
     * @throws Exception if operation fails
     */
    public <T> T executeChain(RequestContext context, Operation<T> operation) throws Exception {
        // Phase 1: Execute before interceptors
        if (!executeBefore(context)) {
            log.warn("Request blocked by interceptor chain");
            throw new InterceptorException("Request blocked by interceptor");
        }

        T result = null;
        Exception caughtException = null;

        try {
            // Phase 2: Execute target operation
            log.debug("Executing target operation");
            result = operation.execute();

            // Phase 3: Execute after interceptors (only if operation succeeded)
            executeAfter(context);

        } catch (Exception ex) {
            log.error("Exception during operation: {}", ex.getMessage());
            caughtException = ex;
        } finally {
            // Phase 4: Always execute after completion (cleanup)
            context.markComplete();
            executeAfterCompletion(context, caughtException);
        }

        // Rethrow exception if operation failed
        if (caughtException != null) {
            throw caughtException;
        }

        return result;
    }

    /**
     * Sort interceptors by order value (ascending)
     */
    private void sortByOrder() {
        interceptors.sort(Comparator.comparingInt(Interceptor::getOrder));
    }

    /**
     * Get list of all interceptors
     */
    public List<Interceptor> getInterceptors() {
        return new ArrayList<>(interceptors);
    }

    /**
     * Get interceptor names (for debugging)
     */
    public List<String> getInterceptorNames() {
        return interceptors.stream()
            .map(Interceptor::getName)
            .collect(Collectors.toList());
    }

    /**
     * Get number of interceptors
     */
    public int size() {
        return interceptors.size();
    }

    /**
     * Clear all interceptors
     */
    public void clear() {
        interceptors.clear();
        executedInterceptors.clear();
    }

    /**
     * Functional interface for operations that can be wrapped by interceptors
     */
    @FunctionalInterface
    public interface Operation<T> {
        T execute() throws Exception;
    }

    /**
     * Exception thrown when interceptor blocks request
     */
    public static class InterceptorException extends RuntimeException {
        public InterceptorException(String message) {
            super(message);
        }

        public InterceptorException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
