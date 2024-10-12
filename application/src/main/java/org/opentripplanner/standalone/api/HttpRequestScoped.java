package org.opentripplanner.standalone.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods in the context as request-scoped. There are special constraints
 * that apply to the components returned by these methods. For example access to these methods
 * are not thread safe, because they are lazy-initialized. See {@link OtpServerRequestContext}.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.SOURCE)
@Inherited
public @interface HttpRequestScoped {
}
