package org.opentripplanner.framework.transaction.configure;

import jakarta.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifies the {@link org.opentripplanner.framework.transaction.RepositoryRegistry} and
 * {@link org.opentripplanner.framework.transaction.UpdateManager} belonging to the transit write
 * domain: timetable data, alerts and realtime vehicles, updated by the SIRI and GTFS-RT updaters.
 * <p>
 * Each write domain has its own registry, transaction sequence and single writer thread, so
 * updaters working on unrelated domains run in parallel. See also {@link StreetDomain}.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
public @interface TransitDomain {}
