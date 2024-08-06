package org.opentripplanner.routing.alertpatch;

import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Direction;

/**
 * This encompasses many different kinds of entity keys, all of which are simple record types, all
 * grouped together as the only allowed implementations of a sealed marker interface. These key
 * types represent various combinations used to look up Alerts that might be associated with a
 * particular stop, or a stop on a route, or all routes of a certain type etc.
 */
public sealed interface EntityKey {
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
