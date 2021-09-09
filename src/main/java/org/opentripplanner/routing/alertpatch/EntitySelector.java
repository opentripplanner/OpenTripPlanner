package org.opentripplanner.routing.alertpatch;

import java.util.Objects;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.ServiceDate;

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
    public final ServiceDate serviceDate;

    private transient int hash = -1;

    public Trip(FeedScopedId tripId) {this(tripId, null);}
    public Trip(FeedScopedId tripId, ServiceDate serviceDate) {
      this.tripId = tripId;
      this.serviceDate = serviceDate;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) { return true; }
      if (o == null || getClass() != o.getClass()) { return false; }
      Trip trip = (Trip) o;

      if ((serviceDate != null && trip.serviceDate != null) &&
          !serviceDate.equals(trip.serviceDate)) {
        // Only compare serviceDate when NOT null
        return false;
      }

      return tripId.equals(trip.tripId);
    }

    @Override
    public int hashCode() {
      if ( hash == -1) {
        int serviceDateResult = serviceDate == null ? 0 : serviceDate.hashCode();
        hash = 31 * serviceDateResult + tripId.hashCode();
      }
      return hash;
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
      this(stopId, tripId, null);
    }

    public StopAndTrip(FeedScopedId stopId, FeedScopedId tripId, ServiceDate serviceDate) {
      this.stopAndTrip = new StopAndRouteOrTripKey(stopId, tripId, serviceDate);
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
    private final ServiceDate serviceDate;
    private final transient int hash;

    public StopAndRouteOrTripKey(FeedScopedId stop, FeedScopedId routeOrTrip) {
      this(stop, routeOrTrip, null);
    }
    public StopAndRouteOrTripKey(FeedScopedId stop, FeedScopedId routeOrTrip, ServiceDate serviceDate) {
      this.stop = stop;
      this.routeOrTrip = routeOrTrip;
      this.serviceDate = serviceDate;
      this.hash = Objects.hash(stop, serviceDate, routeOrTrip);
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

    @Override
    public int hashCode() {
      return hash;
    }
  }
}
