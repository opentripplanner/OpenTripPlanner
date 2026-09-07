package org.opentripplanner.service.transitalert;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.timetable.Direction;

/**
 * An immutable, read-only snapshot of the transit alerts of all sources (updaters). A new snapshot
 * is published each time a transaction that touched the {@link TransitAlertRepository} commits.
 * Request threads read a snapshot resolved at the start of the request, usually through the
 * request-scoped {@link TransitAlertService}.
 * <p>
 * The alerts are indexed by the transit entities they affect, so that the - usually empty - set of
 * alerts for a given stop, route or trip can be looked up quickly while decorating an itinerary.
 */
public interface TransitAlertRepositorySnapshot {
  /**
   * Return all alerts.
   */
  Collection<TransitAlert> getAllAlerts();

  /**
   * Return the alert with the given id, or {@code null} if no such alert exists. There is no
   * guarantee on the stability of the ids.
   */
  @Nullable
  TransitAlert getAlertById(FeedScopedId id);

  /**
   * Returns the alerts for the exact stop only. Alerts on the parent station (or any other related
   * stop) are not included; use {@link #getStopLocationsAlerts} to get alerts for multiple
   * locations at once.
   */
  Collection<TransitAlert> getStopAlerts(FeedScopedId stop, Set<StopCondition> stopConditions);

  /**
   * Returns the alerts for the given stop locations.
   */
  Set<TransitAlert> getStopLocationsAlerts(List<FeedScopedId> stopLocationIds);

  Collection<TransitAlert> getRouteAlerts(FeedScopedId route);

  /**
   * Get trip alerts for any date.
   */
  Collection<TransitAlert> getTripAlerts(FeedScopedId trip);

  Collection<TransitAlert> getTripAlerts(FeedScopedId trip, LocalDate serviceDate);

  Collection<TransitAlert> getAgencyAlerts(FeedScopedId agency);

  Collection<TransitAlert> getStopAndRouteAlerts(
    FeedScopedId stop,
    FeedScopedId route,
    Set<StopCondition> stopConditions,
    Direction direction
  );

  Collection<TransitAlert> getStopAndTripAlerts(
    FeedScopedId stop,
    FeedScopedId trip,
    LocalDate serviceDate,
    Set<StopCondition> stopConditions
  );

  Collection<TransitAlert> getRouteTypeAndAgencyAlerts(int routeType, FeedScopedId agency);

  Collection<TransitAlert> getRouteTypeAlerts(int routeType, String feedId);

  Collection<TransitAlert> getDirectionAndRouteAlerts(Direction direction, FeedScopedId route);
}
