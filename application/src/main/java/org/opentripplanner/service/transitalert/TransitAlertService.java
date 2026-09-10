package org.opentripplanner.service.transitalert;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.api.request.TransitAlertRequest;
import org.opentripplanner.transit.model.filter.transit.TransitAlertMatcherFactory;
import org.opentripplanner.transit.model.timetable.Direction;

/**
 * A TransitAlertService gives read access to the set of alerts (passenger-facing textual
 * information associated with transit entities such as stops or routes) which are currently active
 * and should be provided to end users when their itineraries include the relevant stop, route,
 * etc.
 * <p>
 * It is a read-only, request-scoped view over a {@link TransitAlertRepositorySnapshot}. A new
 * instance should be created for each request, with a snapshot resolved from the request's
 * transaction scope, so that the whole request sees one consistent view of the realtime data.
 */
public interface TransitAlertService {
  Collection<TransitAlert> getAllAlerts();

  /**
   * Returns all alerts matching the given request. A request without filters matches all alerts.
   */
  default Collection<TransitAlert> findAlerts(TransitAlertRequest request) {
    var alerts = getAllAlerts();
    if (request.filters().isEmpty()) {
      return alerts;
    }
    var matcher = TransitAlertMatcherFactory.of(request);
    return alerts.stream().filter(matcher::match).toList();
  }

  @Nullable
  TransitAlert getAlertById(FeedScopedId id);

  default Collection<TransitAlert> getStopAlerts(FeedScopedId stop) {
    return getStopAlerts(stop, Set.of());
  }

  /**
   * Returns the alerts for the exact stop only. Alerts on the parent station (or any other related
   * stop) are not included; use {@link #getStopLocationsAlerts} to get alerts for multiple
   * locations at once.
   */
  Collection<TransitAlert> getStopAlerts(FeedScopedId stop, Set<StopCondition> stopConditions);

  /**
   * Returns the alerts for the stop locations.
   */
  Set<TransitAlert> getStopLocationsAlerts(List<FeedScopedId> stopLocationIds);

  Collection<TransitAlert> getRouteAlerts(FeedScopedId route);

  /**
   * Get Trip alerts for any date
   */
  Collection<TransitAlert> getTripAlerts(FeedScopedId trip);

  Collection<TransitAlert> getTripAlerts(FeedScopedId trip, LocalDate serviceDate);

  Collection<TransitAlert> getAgencyAlerts(FeedScopedId agency);

  default Collection<TransitAlert> getStopAndRouteAlerts(FeedScopedId stop, FeedScopedId route) {
    return getStopAndRouteAlerts(stop, route, Set.of(), Direction.UNKNOWN);
  }

  Collection<TransitAlert> getStopAndRouteAlerts(
    FeedScopedId stop,
    FeedScopedId route,
    Set<StopCondition> stopConditions,
    Direction direction
  );

  default Collection<TransitAlert> getStopAndTripAlerts(
    FeedScopedId stop,
    FeedScopedId trip,
    LocalDate serviceDate
  ) {
    return getStopAndTripAlerts(stop, trip, serviceDate, Set.of());
  }

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
