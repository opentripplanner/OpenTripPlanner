package org.opentripplanner.transit.configure;

import jakarta.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifies the app-singleton {@link org.opentripplanner.transit.service.TransitService} binding
 * that has no real-time data — for consumers that live outside any HTTP request. Distinguishes it
 * from the unqualified, request-scoped {@code TransitService} binding inside {@link
 * org.opentripplanner.standalone.configure.RequestScopedComponent}, since Dagger does not support
 * a subcomponent overriding an ancestor's binding for the same, unqualified type.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
public @interface StaticTransitService {}
