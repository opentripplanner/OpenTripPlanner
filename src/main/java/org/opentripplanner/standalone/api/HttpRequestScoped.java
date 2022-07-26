package org.opentripplanner.standalone.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods in the context as request-scoped. There are special constraints
 * that apply to the components returned by these methods. For example access to these methods
 * are not thread safe, because thy are lazy-initialized. See {@link OtpServerContext}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface HttpRequestScoped {
}
