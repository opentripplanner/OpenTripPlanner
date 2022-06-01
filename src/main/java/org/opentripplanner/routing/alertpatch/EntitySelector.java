package org.opentripplanner.routing.alertpatch;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.transit.model.basic.FeedScopedId;

public interface EntitySelector {
  class Agency implements EntitySelector {

    public final FeedScopedId agencyId;

    public Agency(FeedScopedId agencyId) {
      this.agencyId = agencyId;
    }

    @Override
    public int hashCode() {
      return agencyId.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Agency agency = (Agency) o;
      return agencyId.equals(agency.agencyId);
    }
  }

  class Stop implements EntitySelector {

    public final FeedScopedId stopId;
    public final Set<StopCondition> stopConditions;

    public Stop(FeedScopedId stopId) {
      this(stopId, Collections.EMPTY_SET);
    }

    public Stop(FeedScopedId stopId, Set<StopCondition> stopConditions) {
      this.stopId = stopId;
      this.stopConditions = stopConditions;
    }

    @Override
    public int hashCode() {
      return stopId.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Stop stop = (Stop) o;
      return stopId.equals(stop.stopId);
    }
  }

  class Route implements EntitySelector {

    public final FeedScopedId routeId;

    public Route(FeedScopedId routeId) {
      this.routeId = routeId;
    }

    @Override
    public int hashCode() {
      return routeId.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Route route = (Route) o;
      return routeId.equals(route.routeId);
    }
  }

  class Trip implements EntitySelector {

    public final FeedScopedId tripId;
    public final ServiceDate serviceDate;

    private transient int hash = -1;

    public Trip(FeedScopedId tripId) {
      this(tripId, null);
    }

    public Trip(FeedScopedId tripId, ServiceDate serviceDate) {
      this.tripId = tripId;
      this.serviceDate = serviceDate;
    }

    @Override
    public int hashCode() {
      if (hash == -1) {
        int serviceDateResult = serviceDate == null ? 0 : serviceDate.hashCode();
        hash = 31 * serviceDateResult + tripId.hashCode();
      }
      return hash;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Trip trip = (Trip) o;

      if (
        (serviceDate != null && trip.serviceDate != null) && !serviceDate.equals(trip.serviceDate)
      ) {
        // Only compare serviceDate when NOT null
        return false;
      }

      return tripId.equals(trip.tripId);
    }
  }

  class StopAndRoute implements EntitySelector {

    public final StopAndRouteOrTripKey stopAndRoute;

    public StopAndRoute(FeedScopedId stopId, FeedScopedId routeId) {
      this.stopAndRoute = new StopAndRouteOrTripKey(stopId, Collections.EMPTY_SET, routeId);
    }

    public StopAndRoute(
      FeedScopedId stopId,
      Set<StopCondition> stopConditions,
      FeedScopedId routeId
    ) {
      this.stopAndRoute = new StopAndRouteOrTripKey(stopId, stopConditions, routeId);
    }

    @Override
    public int hashCode() {
      return stopAndRoute.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      StopAndRoute that = (StopAndRoute) o;
      return stopAndRoute.equals(that.stopAndRoute);
    }
  }

  class StopAndTrip implements EntitySelector {

    public final StopAndRouteOrTripKey stopAndTrip;

    public StopAndTrip(FeedScopedId stopId, FeedScopedId tripId) {
      this(stopId, Collections.EMPTY_SET, tripId, null);
    }

    public StopAndTrip(FeedScopedId stopId, FeedScopedId tripId, ServiceDate serviceDate) {
      this.stopAndTrip =
        new StopAndRouteOrTripKey(stopId, Collections.EMPTY_SET, tripId, serviceDate);
    }

    public StopAndTrip(
      FeedScopedId stopId,
      Set<StopCondition> stopConditions,
      FeedScopedId tripId,
      ServiceDate serviceDate
    ) {
      this.stopAndTrip = new StopAndRouteOrTripKey(stopId, stopConditions, tripId, serviceDate);
    }

    @Override
    public int hashCode() {
      return stopAndTrip.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      StopAndTrip that = (StopAndTrip) o;
      return stopAndTrip.equals(that.stopAndTrip);
    }
  }

  class Unknown implements EntitySelector {

    public final String description;

    public Unknown(String description) {
      this.description = description;
    }

    @Override
    public int hashCode() {
      return Objects.hash(description);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Unknown that = (Unknown) o;
      return description.equals(that.description);
    }
  }

  class RouteType implements EntitySelector {

    public final int routeType;

    public final String feedId;

    public RouteType(int routeType, String feedId) {
      this.routeType = routeType;
      this.feedId = feedId;
    }

    @Override
    public int hashCode() {
      return 37 * routeType * Objects.hash(feedId);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      RouteType that = (RouteType) o;
      return routeType == that.routeType && feedId.equals(that.feedId);
    }
  }

  class RouteTypeAndAgency implements EntitySelector {

    public final int routeType;

    public final FeedScopedId agencyId;

    public RouteTypeAndAgency(int routeType, FeedScopedId agencyId) {
      this.routeType = routeType;
      this.agencyId = agencyId;
    }

    @Override
    public int hashCode() {
      int agencyHash = Objects.hash(agencyId);
      return 37 * routeType * agencyHash;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      RouteTypeAndAgency that = (RouteTypeAndAgency) o;
      return routeType == that.routeType && agencyId.equals(that.agencyId);
    }
  }

  class DirectionAndRoute implements EntitySelector {

    public final int directionId;

    public final FeedScopedId routeId;

    public DirectionAndRoute(int directionId, FeedScopedId routeId) {
      this.directionId = directionId;
      this.routeId = routeId;
    }

    @Override
    public int hashCode() {
      int routeHash = Objects.hash(routeId);
      return 41 * directionId * routeHash;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      DirectionAndRoute that = (DirectionAndRoute) o;
      return directionId == that.directionId && routeId.equals(that.routeId);
    }
  }

  class StopAndRouteOrTripKey {

    public final FeedScopedId stop;
    public final FeedScopedId routeOrTrip;
    public final ServiceDate serviceDate;
    public final Set<StopCondition> stopConditions;
    private final transient int hash;

    public StopAndRouteOrTripKey(
      FeedScopedId stop,
      Set<StopCondition> stopConditions,
      FeedScopedId routeOrTrip
    ) {
      this(stop, stopConditions, routeOrTrip, null);
    }

    public StopAndRouteOrTripKey(
      FeedScopedId stop,
      Set<StopCondition> stopConditions,
      FeedScopedId routeOrTrip,
      ServiceDate serviceDate
    ) {
      this.stop = stop;
      this.routeOrTrip = routeOrTrip;
      this.serviceDate = serviceDate;
      this.stopConditions = stopConditions;
      this.hash = Objects.hash(stop, serviceDate, routeOrTrip);
    }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      StopAndRouteOrTripKey that = (StopAndRouteOrTripKey) o;

      if (!stop.equals(that.stop)) {
        return false;
      }

      if (!routeOrTrip.equals(that.routeOrTrip)) {
        return false;
      }

      return serviceDate != null ? serviceDate.equals(that.serviceDate) : that.serviceDate == null;
    }
  }
}
