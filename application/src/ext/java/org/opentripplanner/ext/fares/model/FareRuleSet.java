package org.opentripplanner.ext.fares.model;

import java.io.Serializable;
import java.time.Duration;
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

  public FareRuleSet(FareAttribute fareAttribute) {
    this.fareAttribute = fareAttribute;
    routes = new HashSet<>();
    originDestinations = new HashSet<>();
    routeOriginDestinations = new HashSet<>();
    contains = new HashSet<>();
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

  /**
   * Determine whether the FareRuleSet has any rules added.
   * @return True if any rules have been added.
   */
  public boolean hasRules() {
    return (
      !routes.isEmpty() ||
      !originDestinations.isEmpty() ||
      !routeOriginDestinations.isEmpty() ||
      !contains.isEmpty()
    );
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

  /**
   * Determines whether the FareRuleSet matches against a set of itinerary parameters
   * based on the added rules and fare attribute
   * @param startZone Origin zone
   * @param endZone End zone
   * @param zonesVisited A set containing the names of zones visited on the fare
   * @param routesVisited A set containing the route IDs visited
   * @param tripsVisited [Not implemented] A set containing the trip IDs visited
   * @param transfersUsed Number of transfers already used
   * @param tripTime Time from beginning of first leg to beginning of current leg to be evaluated
   * @param journeyTime Total journey time from beginning of first leg to end of current leg
   * @return True if this FareAttribute should apply to this leg
   */
  public boolean matches(
    String startZone,
    String endZone,
    Set<String> zonesVisited,
    Set<FeedScopedId> routesVisited,
    Set<FeedScopedId> tripsVisited,
    int transfersUsed,
    Duration tripTime,
    Duration journeyTime
  ) {
    //check for matching origin/destination, if this ruleset has any origin/destination restrictions
    if (!originDestinations.isEmpty()) {
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
    if (!contains.isEmpty()) {
      if (!zonesVisited.equals(contains)) {
        return false;
      }
    }

    //check for matching routes
    if (!routes.isEmpty()) {
      if (!routes.containsAll(routesVisited)) {
        return false;
      }
    }

    if (fareAttribute.isTransfersSet() && fareAttribute.getTransfers() < transfersUsed) {
      return false;
    }
    // assume transfers are evaluated at boarding time,
    // as trimet does
    if (
      fareAttribute.isTransferDurationSet() &&
      tripTime.getSeconds() > fareAttribute.getTransferDuration()
    ) {
      return false;
    }
    if (
      fareAttribute.isJourneyDurationSet() &&
      journeyTime.getSeconds() > fareAttribute.getJourneyDuration()
    ) {
      return false;
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
