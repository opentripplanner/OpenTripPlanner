package org.opentripplanner.standalone.api;

import jakarta.inject.Scope;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.opentripplanner.standalone.configure.RequestScopedFactory;

/**
 * A real Dagger scope for bindings that must be created exactly once per HTTP request and shared
 * for the lifetime of that request — most importantly the {@link
 * org.opentripplanner.framework.transaction.api.TransactionScope} pinning a consistent view of
 * real-time data, and anything derived from it (e.g. {@link
 * org.opentripplanner.transit.service.TransitService}). See {@link
 * RequestScopedFactory}.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Scope
public @interface HttpRequestScoped {}
