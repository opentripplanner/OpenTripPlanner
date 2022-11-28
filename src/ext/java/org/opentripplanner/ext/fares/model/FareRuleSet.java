package org.opentripplanner.ext.fares.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FareRuleSet implements Serializable {

  private FeedScopedId agency = null;
  private final Set<FeedScopedId> routes;
  private final Set<OriginDestination> originDestinations;

  private final Set<RouteOriginDestination> routeOriginDestinations;
  private final Set<String> contains;
  private final FareAttribute fareAttribute;
  private final Set<FeedScopedId> trips;

  public FareRuleSet(FareAttribute fareAttribute) {
    this.fareAttribute = fareAttribute;
    routes = new HashSet<>();
    originDestinations = new HashSet<>();
    routeOriginDestinations = new HashSet<>();
    contains = new HashSet<>();
    trips = new HashSet<>();
  }

  public void addOriginDestination(String origin, String destination) {
    originDestinations.add(new OriginDestination(origin, destination));
  }

  /**
   * Used for checking ticket validity on HSL special routes on which some tickets are valid
   * outside their normal validity zones
   */
  public void addRouteOriginDestination(String route, String origin, String destination) {
    routeOriginDestinations.add(new RouteOriginDestination(route, origin, destination));
  }

  public Set<RouteOriginDestination> getRouteOriginDestinations() {
    return routeOriginDestinations;
  }

  public void addContains(String containsId) {
    contains.add(containsId);
  }

  public Set<String> getContains() {
    return contains;
  }

  public void addRoute(FeedScopedId route) {
    routes.add(route);
  }

  public Set<FeedScopedId> getRoutes() {
    return routes;
  }

  public FareAttribute getFareAttribute() {
    return fareAttribute;
  }

  public void addTrip(FeedScopedId trip) {
    trips.add(trip);
  }

  public Set<FeedScopedId> getTrips() {
    return trips;
  }

  public boolean matches(
    String startZone,
    String endZone,
    Set<String> zonesVisited,
    Set<FeedScopedId> routesVisited,
    Set<FeedScopedId> tripsVisited
  ) {
    //check for matching origin/destination, if this ruleset has any origin/destination restrictions
    if (originDestinations.size() > 0) {
      var od = new OriginDestination(startZone, endZone);
      if (!originDestinations.contains(od)) {
        var od2 = new OriginDestination(od.origin, null);
        if (!originDestinations.contains(od2)) {
          od2 = new OriginDestination(null, od.origin);
          if (!originDestinations.contains(od2)) {
            return false;
          }
        }
      }
    }

    //check for matching contains, if this ruleset has any containment restrictions
    if (contains.size() > 0) {
      if (!zonesVisited.equals(contains)) {
        return false;
      }
    }

    //check for matching routes
    if (routes.size() != 0) {
      if (!routes.containsAll(routesVisited)) {
        return false;
      }
    }

    //check for matching trips
    if (trips.size() != 0) {
      if (!trips.containsAll(tripsVisited)) {
        return false;
      }
    }

    return true;
  }

  public boolean hasAgencyDefined() {
    return this.agency != null;
  }

  public FeedScopedId getAgency() {
    return this.agency;
  }

  public void setAgency(FeedScopedId agency) {
    this.agency = agency;
  }

  record OriginDestination(String origin, String destination) {}
}
