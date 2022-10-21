package org.opentripplanner.routing.services;

import java.time.LocalDate;
import java.util.Collection;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Direction;

public interface TransitAlertService {
  void setAlerts(Collection<TransitAlert> alerts);

  Collection<TransitAlert> getAllAlerts();

  TransitAlert getAlertById(String id);

  Collection<TransitAlert> getStopAlerts(FeedScopedId stop);

  Collection<TransitAlert> getRouteAlerts(FeedScopedId route);

  Collection<TransitAlert> getTripAlerts(FeedScopedId trip, LocalDate serviceDate);

  Collection<TransitAlert> getAgencyAlerts(FeedScopedId agency);

  Collection<TransitAlert> getStopAndRouteAlerts(FeedScopedId stop, FeedScopedId route);

  Collection<TransitAlert> getStopAndTripAlerts(
    FeedScopedId stop,
    FeedScopedId trip,
    LocalDate serviceDate
  );

  Collection<TransitAlert> getRouteTypeAndAgencyAlerts(int routeType, FeedScopedId agency);

  Collection<TransitAlert> getRouteTypeAlerts(int routeType, String feedId);

  Collection<TransitAlert> getDirectionAndRouteAlerts(Direction direction, FeedScopedId route);
}
