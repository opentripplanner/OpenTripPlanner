package org.opentripplanner.framework.transaction.configure;

import jakarta.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifies the {@link org.opentripplanner.framework.transaction.RepositoryRegistry} and
 * {@link org.opentripplanner.framework.transaction.UpdateManager} belonging to the alert write
 * domain: the transit alerts, updated by the GTFS-RT alert and SIRI-SX updaters.
 * <p>
 * The alerts have a registry of their own - separate from the {@link TransitDomain} - so that they
 * can be committed atomically after every update task without forcing the timetable to publish a
 * new snapshot (and re-index Raptor) at the same rate. The price of the separation is that alert
 * tasks resolve transit entities against the last <em>committed</em> timetable snapshot rather than
 * the timetable write buffer.
 *
 * @see StreetDomain
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
public @interface AlertDomain {}
