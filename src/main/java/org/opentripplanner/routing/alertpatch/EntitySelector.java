package org.opentripplanner.routing.alertpatch;

import org.opentripplanner.model.FeedScopedId;

public interface EntitySelector {
  class Agency implements EntitySelector {
    public final FeedScopedId agencyId;

    public Agency(FeedScopedId agencyId) {this.agencyId = agencyId;}

    @Override
    public boolean equals(Object o) {
      if (this == o) { return true; }
      if (o == null || getClass() != o.getClass()) { return false; }
      Agency agency = (Agency) o;
      return agencyId.equals(agency.agencyId);
    }

    @Override
    public int hashCode() {
      return agencyId.hashCode();
    }
  }

  class Stop implements EntitySelector {
    public final FeedScopedId stopId;

    public Stop(FeedScopedId stopId) {this.stopId = stopId;}

    @Override
    public boolean equals(Object o) {
      if (this == o) { return true; }
      if (o == null || getClass() != o.getClass()) { return false; }
      Stop stop = (Stop) o;
      return stopId.equals(stop.stopId);
    }

    @Override
    public int hashCode() {
      return stopId.hashCode();
    }
  }

  class Route implements EntitySelector {
    public final FeedScopedId routeId;

    public Route(FeedScopedId routeId) {this.routeId = routeId;}

    @Override
    public boolean equals(Object o) {
      if (this == o) { return true; }
      if (o == null || getClass() != o.getClass()) { return false; }
      Route route = (Route) o;
      return routeId.equals(route.routeId);
    }

    @Override
    public int hashCode() {
      return routeId.hashCode();
    }
  }

  class Trip implements EntitySelector {
    public final FeedScopedId tripId;

    public Trip(FeedScopedId tripId) {this.tripId = tripId;}

    @Override
    public boolean equals(Object o) {
      if (this == o) { return true; }
      if (o == null || getClass() != o.getClass()) { return false; }
      Trip trip = (Trip) o;
      return tripId.equals(trip.tripId);
    }

    @Override
    public int hashCode() {
      return tripId.hashCode();
    }
  }

  class TripPattern implements EntitySelector {
    public final FeedScopedId tripPatternId;

    public TripPattern(FeedScopedId tripPatternId) {this.tripPatternId = tripPatternId;}

    @Override
    public boolean equals(Object o) {
      if (this == o) { return true; }
      if (o == null || getClass() != o.getClass()) { return false; }
      TripPattern that = (TripPattern) o;
      return tripPatternId.equals(that.tripPatternId);
    }

    @Override
    public int hashCode() {
      return tripPatternId.hashCode();
    }
  }

  class StopAndRoute implements EntitySelector {
    public final StopAndRouteOrTripKey stopAndRoute;

    public StopAndRoute(FeedScopedId stopId, FeedScopedId routeId) {
      this.stopAndRoute = new StopAndRouteOrTripKey(stopId, routeId);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) { return true; }
      if (o == null || getClass() != o.getClass()) { return false; }
      StopAndRoute that = (StopAndRoute) o;
      return stopAndRoute.equals(that.stopAndRoute);
    }

    @Override
    public int hashCode() {
      return stopAndRoute.hashCode();
    }
  }

  class StopAndTrip implements EntitySelector {
    public final StopAndRouteOrTripKey stopAndTrip;

    public StopAndTrip(FeedScopedId stopId, FeedScopedId tripId) {
      this.stopAndTrip = new StopAndRouteOrTripKey(stopId, tripId);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) { return true; }
      if (o == null || getClass() != o.getClass()) { return false; }
      StopAndTrip that = (StopAndTrip) o;
      return stopAndTrip.equals(that.stopAndTrip);
    }

    @Override
    public int hashCode() {
      return stopAndTrip.hashCode();
    }
  }

  class StopAndRouteOrTripKey {
    private final FeedScopedId stop;
    private final FeedScopedId routeOrTrip;
    private transient int hash = 0;

    public StopAndRouteOrTripKey(FeedScopedId stop, FeedScopedId routeOrTrip) {
      this.stop = stop;
      this.routeOrTrip = routeOrTrip;
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
      return routeOrTrip.equals(that.routeOrTrip);
    }

    @Override
    public int hashCode() {
      if (hash == 0) {
        int result = stop.hashCode();
        hash = 31 * result + routeOrTrip.hashCode();
      }
      return hash;
    }
  }
}
