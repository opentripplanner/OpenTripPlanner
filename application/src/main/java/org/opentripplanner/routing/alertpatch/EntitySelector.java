package org.opentripplanner.routing.alertpatch;

import java.time.LocalDate;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Direction;

/**
 * Describes which elements in the internal transit data model are affected by a realtime alert.
 * Note that this is specific to alerts and doesn't seem to be used by anything else.
 * This is probably because alerts are unique in their ability to attach themselves to many
 * different routes, stops, etc. at once, while non-alert elements tend to be associated with very
 * specific single other elements.
 * @see EntityKey
 */
public sealed interface EntitySelector {
  EntityKey key();

  default boolean matches(EntitySelector other) {
    return this.equals(other);
  }

  record Agency(FeedScopedId agencyId) implements EntitySelector {
    @Override
    public EntityKey.Agency key() {
      return new EntityKey.Agency(agencyId);
    }
  }

  record Stop(FeedScopedId stopId, Set<StopCondition> stopConditions) implements EntitySelector {
    public Stop(FeedScopedId stopId) {
      this(stopId, Set.of());
    }

    @Override
    public EntityKey.Stop key() {
      return new EntityKey.Stop(stopId);
    }

    @Override
    public boolean matches(EntitySelector other) {
      if (!(other instanceof EntitySelector.Stop s)) {
        return false;
      }
      return (
        stopId.equals(s.stopId) &&
        StopConditionsHelper.matchesStopCondition(stopConditions, s.stopConditions)
      );
    }
  }

  record Route(FeedScopedId routeId) implements EntitySelector {
    @Override
    public EntityKey.Route key() {
      return new EntityKey.Route(routeId);
    }
  }

  record Trip(FeedScopedId tripId, @Nullable LocalDate serviceDate) implements EntitySelector {
    public Trip(FeedScopedId tripId) {
      this(tripId, null);
    }

    @Override
    public EntityKey.Trip key() {
      return new EntityKey.Trip(tripId);
    }

    @Override
    public boolean matches(EntitySelector other) {
      if (!(other instanceof EntitySelector.Trip t)) {
        return false;
      }
      return (
        tripId.equals(t.tripId) &&
        (serviceDate == null || t.serviceDate == null || serviceDate.equals(t.serviceDate))
      );
    }
  }

  record StopAndRoute(FeedScopedId stopId, FeedScopedId routeId, Set<StopCondition> stopConditions)
    implements EntitySelector {
    public StopAndRoute(FeedScopedId stopId, FeedScopedId routeId) {
      this(stopId, routeId, Set.of());
    }

    public StopAndRoute(
      FeedScopedId stopId,
      Set<StopCondition> stopConditions,
      FeedScopedId routeId
    ) {
      this(stopId, routeId, stopConditions);
    }

    @Override
    public EntityKey.StopAndRoute key() {
      return new EntityKey.StopAndRoute(stopId, routeId);
    }

    @Override
    public boolean matches(EntitySelector other) {
      if (!(other instanceof EntitySelector.StopAndRoute s)) {
        return false;
      }
      return (
        stopId.equals(s.stopId) &&
        routeId.equals(s.routeId) &&
        StopConditionsHelper.matchesStopCondition(stopConditions, s.stopConditions)
      );
    }
  }

  record StopAndTrip(
    FeedScopedId stopId,
    FeedScopedId tripId,
    LocalDate serviceDate,
    Set<StopCondition> stopConditions
  )
    implements EntitySelector {
    public StopAndTrip(FeedScopedId stopId, FeedScopedId tripId) {
      this(stopId, tripId, null, Set.of());
    }

    public StopAndTrip(FeedScopedId stopId, FeedScopedId tripId, LocalDate serviceDate) {
      this(stopId, tripId, serviceDate, Set.of());
    }

    @Override
    public EntityKey.StopAndTrip key() {
      return new EntityKey.StopAndTrip(stopId, tripId);
    }

    @Override
    public boolean matches(EntitySelector other) {
      if (!(other instanceof EntitySelector.StopAndTrip s)) {
        return false;
      }
      return (
        stopId.equals(s.stopId) &&
        tripId.equals(s.tripId) &&
        StopConditionsHelper.matchesStopCondition(stopConditions, s.stopConditions) &&
        (serviceDate == null || s.serviceDate == null || serviceDate.equals(s.serviceDate))
      );
    }
  }

  record Unknown(String description) implements EntitySelector {
    @Override
    public EntityKey.Unknown key() {
      return new EntityKey.Unknown();
    }
  }

  record RouteType(String feedId, int routeType) implements EntitySelector {
    @Override
    public EntityKey.RouteType key() {
      return new EntityKey.RouteType(feedId, routeType);
    }
  }

  record RouteTypeAndAgency(FeedScopedId agencyId, int routeType) implements EntitySelector {
    @Override
    public EntityKey.RouteTypeAndAgency key() {
      return new EntityKey.RouteTypeAndAgency(agencyId, routeType);
    }
  }

  record DirectionAndRoute(FeedScopedId routeId, Direction direction) implements EntitySelector {
    @Override
    public EntityKey.DirectionAndRoute key() {
      return new EntityKey.DirectionAndRoute(routeId, direction);
    }
  }
}
