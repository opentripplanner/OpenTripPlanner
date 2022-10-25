package org.opentripplanner.routing.alertpatch;

import java.time.LocalDate;
import java.util.Set;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Direction;

public interface EntitySelector {
  EntityKey key();

  record Agency(FeedScopedId agencyId) implements EntitySelector {
    @Override
    public EntityKey key() {
      return new EntityKey.Agency(agencyId);
    }
  }

  record Stop(FeedScopedId stopId, Set<StopCondition> stopConditions) implements EntitySelector {
    public Stop(FeedScopedId stopId) {
      this(stopId, Set.of());
    }

    @Override
    public EntityKey key() {
      return new EntityKey.Stop(stopId);
    }
  }

  record Route(FeedScopedId routeId) implements EntitySelector {
    @Override
    public EntityKey key() {
      return new EntityKey.Route(routeId);
    }
  }

  record Trip(FeedScopedId tripId, LocalDate serviceDate) implements EntitySelector {
    public Trip(FeedScopedId tripId) {
      this(tripId, null);
    }

    @Override
    public EntityKey key() {
      return new EntityKey.Trip(tripId);
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
    public EntityKey key() {
      return new EntityKey.StopAndRoute(stopId, routeId);
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
    public EntityKey key() {
      return new EntityKey.StopAndTrip(stopId, tripId);
    }
  }

  record Unknown(String description) implements EntitySelector {
    @Override
    public EntityKey key() {
      return new EntityKey.Unknown();
    }
  }

  record RouteType(String feedId, int routeType) implements EntitySelector {
    @Override
    public EntityKey key() {
      return new EntityKey.RouteType(feedId, routeType);
    }
  }

  record RouteTypeAndAgency(FeedScopedId agencyId, int routeType) implements EntitySelector {
    @Override
    public EntityKey key() {
      return new EntityKey.RouteTypeAndAgency(agencyId, routeType);
    }
  }

  record DirectionAndRoute(FeedScopedId routeId, Direction direction) implements EntitySelector {
    @Override
    public EntityKey key() {
      return new EntityKey.DirectionAndRoute(routeId, direction);
    }
  }
}
