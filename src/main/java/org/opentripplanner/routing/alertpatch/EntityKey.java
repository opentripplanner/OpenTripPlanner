package org.opentripplanner.routing.alertpatch;

import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Direction;

public interface EntityKey {
  record Agency(FeedScopedId agencyId) implements EntityKey {}

  record Stop(FeedScopedId stopId) implements EntityKey {}

  record Route(FeedScopedId routeId) implements EntityKey {}

  record Trip(FeedScopedId tripId) implements EntityKey {}

  record StopAndRoute(FeedScopedId stopId, FeedScopedId routeId) implements EntityKey {}

  record StopAndTrip(FeedScopedId stopId, FeedScopedId tripId) implements EntityKey {}

  record Unknown() implements EntityKey {}

  record RouteType(String feedId, int routeType) implements EntityKey {}

  record RouteTypeAndAgency(FeedScopedId agencyId, int routeType) implements EntityKey {}

  record DirectionAndRoute(FeedScopedId routeId, Direction direction) implements EntityKey {}
}
