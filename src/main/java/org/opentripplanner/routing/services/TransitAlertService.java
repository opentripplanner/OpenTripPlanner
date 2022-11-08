package org.opentripplanner.routing.services;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import org.opentripplanner.routing.alertpatch.StopCondition;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Direction;

public interface TransitAlertService {
  void setAlerts(Collection<TransitAlert> alerts);

  Collection<TransitAlert> getAllAlerts();

  TransitAlert getAlertById(String id);

  default Collection<TransitAlert> getStopAlerts(FeedScopedId stop) {
    return getStopAlerts(stop, Set.of());
  }

  Collection<TransitAlert> getStopAlerts(FeedScopedId stop, Set<StopCondition> stopConditions);

  Collection<TransitAlert> getRouteAlerts(FeedScopedId route);

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
