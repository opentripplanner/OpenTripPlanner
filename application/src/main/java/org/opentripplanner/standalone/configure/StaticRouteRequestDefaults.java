package org.opentripplanner.standalone.configure;

import jakarta.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifies the raw, app-singleton {@link org.opentripplanner.routing.api.request.RouteRequest}
 * bound from {@link org.opentripplanner.standalone.config.RouterConfig#routingRequestDefaults()}
 * at startup — used only for one-off, build-time consumers (e.g. GraphQL schema generation).
 * Distinguishes it from the unqualified, request-scoped {@code RouteRequest} inside {@link
 * RequestScopedFactory}, which is a fresh, launcher-decorated copy per HTTP request. Same shape
 * of problem as {@code StaticTransitService} — Dagger does not support a subcomponent overriding
 * an ancestor's binding for the same, unqualified type.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
public @interface StaticRouteRequestDefaults {}
