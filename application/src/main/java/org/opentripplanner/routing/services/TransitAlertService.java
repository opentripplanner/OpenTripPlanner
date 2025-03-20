package org.opentripplanner.routing.services;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Direction;

/**
 * A TransitAlertService stores a set of alerts (passenger-facing textual information associated
 * with transit entities such as stops or routes) which are currently active and should be provided
 * to end users when their itineraries include the relevant stop, route, etc.
 *
 * Its primary purpose is to index those alerts, which may be numerous, so they can be looked up
 * rapidly and attached to the various pieces of an itinerary as it's being returned to the user.
 *
 * Most elements in an itinerary will have no alerts attached, so those cases need to return
 * quickly. For example, no alerts on board stop A, no alerts on route 1 ridden, no alerts on alight
 * stop B, no alerts on route 2 ridden, yes one alert found on alight stop C.
 *
 * The fact that alerts are relatively sparse (at the scale of the entire transportation system)
 * is central to this implementation. Adding a list of alerts to every element in the system would
 * mean storing large amounts of null or empty list references. Instead, alerts are looked up in
 * maps allowing them to be attached to any object with minimal space overhead, but requiring some
 * careful indexing to ensure their presence or absence on each object can be determined quickly.
 */
public interface TransitAlertService {
  void setAlerts(Collection<TransitAlert> alerts);

  Collection<TransitAlert> getAllAlerts();

  TransitAlert getAlertById(FeedScopedId id);

  default Collection<TransitAlert> getStopAlerts(FeedScopedId stop) {
    return getStopAlerts(stop, Set.of());
  }

  Collection<TransitAlert> getStopAlerts(FeedScopedId stop, Set<StopCondition> stopConditions);

  Collection<TransitAlert> getRouteAlerts(FeedScopedId route);

  /**
   * Get Trip alerts for any date
   */
  Collection<TransitAlert> getTripAlerts(FeedScopedId trip);

  Collection<TransitAlert> getTripAlerts(FeedScopedId trip, LocalDate serviceDate);

  Collection<TransitAlert> getAgencyAlerts(FeedScopedId agency);

  default Collection<TransitAlert> getStopAndRouteAlerts(FeedScopedId stop, FeedScopedId route) {
    return getStopAndRouteAlerts(stop, route, Set.of());
  }

  Collection<TransitAlert> getStopAndRouteAlerts(
    FeedScopedId stop,
    FeedScopedId route,
    Set<StopCondition> stopConditions
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
