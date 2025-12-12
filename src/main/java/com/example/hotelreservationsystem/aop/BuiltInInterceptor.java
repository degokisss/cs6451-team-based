package com.example.hotelreservationsystem.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation to enable Spring AOP-based interception for auth endpoints.
 * Apply to controller methods (or classes) where you want the built-in interceptor
 * to run instead of the custom interceptor chain.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface BuiltInInterceptor {
}
