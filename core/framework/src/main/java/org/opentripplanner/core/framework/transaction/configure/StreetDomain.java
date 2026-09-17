package org.opentripplanner.framework.transaction.configure;

import jakarta.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.opentripplanner.core.domain.framework.transaction.RepositoryRegistry;
import org.opentripplanner.core.domain.framework.transaction.UpdateManager;

/**
 * Qualifies the {@link RepositoryRegistry} and
 * {@link UpdateManager} belonging to the street write
 * domain: the street graph and the vehicle-rental and vehicle-parking repositories, updated by
 * the GBFS and parking updaters.
 * <p>
 * The expensive street work — linking rental vertices and applying geofencing zones — runs on
 * this domain's writer thread and no longer delays timetable updates queued on the transit
 * domain's writer thread. See also {@link TransitDomain}.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
public @interface StreetDomain {}
