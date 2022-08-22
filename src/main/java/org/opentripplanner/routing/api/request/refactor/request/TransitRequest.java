package org.opentripplanner.routing.api.request.refactor.request;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.routing.api.request.DebugRaptor;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;

public class TransitRequest {

  // TODO: 2022-08-17 Should be a set?
  private List<MainAndSubMode> modes = MainAndSubMode.all();
  private Set<FeedScopedId> whiteListedAgencies = Set.of();
  private Set<FeedScopedId> bannedAgencies = Set.of();
  private Set<FeedScopedId> preferredAgencies = Set.of();
  private Set<FeedScopedId> unpreferredAgencies = Set.of();
  private RouteMatcher whiteListedRoutes = RouteMatcher.emptyMatcher();
  private RouteMatcher bannedRoutes = RouteMatcher.emptyMatcher();
  private RouteMatcher preferredRoutes = RouteMatcher.emptyMatcher();
  private RouteMatcher unpreferredRoutes = RouteMatcher.emptyMatcher();
  private Set<FeedScopedId> bannedTrips = Set.of();
  private DebugRaptor raptorDebugging = new DebugRaptor();

  public Set<FeedScopedId> bannedRoutes(Collection<Route> routes) {
    if (
      bannedRoutes.isEmpty() &&
      bannedAgencies.isEmpty() &&
      whiteListedRoutes.isEmpty() &&
      whiteListedAgencies.isEmpty()
    ) {
      return Set.of();
    }

    Set<FeedScopedId> bannedRoutes = new HashSet<>();
    for (Route route : routes) {
      if (routeIsBanned(route)) {
        bannedRoutes.add(route.getId());
      }
    }
    return bannedRoutes;
  }

  /**
   * Checks if the route is banned. Also, if whitelisting is used, the route (or its agency) has to
   * be whitelisted in order to not count as banned.
   *
   * @return True if the route is banned
   */
  private boolean routeIsBanned(Route route) {
    /* check if agency is banned for this plan */
    if (!bannedAgencies.isEmpty()) {
      if (bannedAgencies.contains(route.getAgency().getId())) {
        return true;
      }
    }

    /* check if route banned for this plan */
    if (!bannedRoutes.isEmpty()) {
      if (bannedRoutes.matches(route)) {
        return true;
      }
    }

    boolean whiteListed = false;
    boolean whiteListInUse = false;

    /* check if agency is whitelisted for this plan */
    if (!whiteListedAgencies.isEmpty()) {
      whiteListInUse = true;
      if (whiteListedAgencies.contains(route.getAgency().getId())) {
        whiteListed = true;
      }
    }

    /* check if route is whitelisted for this plan */
    if (!whiteListedRoutes.isEmpty()) {
      whiteListInUse = true;
      if (whiteListedRoutes.matches(route)) {
        whiteListed = true;
      }
    }

    if (whiteListInUse && !whiteListed) {
      return true;
    }

    return false;
  }

  public List<MainAndSubMode> modes() {
    return modes;
  }

  public Set<FeedScopedId> whiteListedAgencies() {
    return whiteListedAgencies;
  }

  public Set<FeedScopedId> bannedAgencies() {
    return bannedAgencies;
  }

  public Set<FeedScopedId> preferredAgencies() {
    return preferredAgencies;
  }

  public Set<FeedScopedId> unpreferredAgencies() {
    return unpreferredAgencies;
  }

  public RouteMatcher whiteListedRoutes() {
    return whiteListedRoutes;
  }

  public RouteMatcher bannedRoutes() {
    return bannedRoutes;
  }

  public RouteMatcher preferredRoutes() {
    return preferredRoutes;
  }

  public RouteMatcher unpreferredRoutes() {
    return unpreferredRoutes;
  }

  public Set<FeedScopedId> bannedTrips() {
    return bannedTrips;
  }

  public DebugRaptor raptorDebugging() {
    return raptorDebugging;
  }
}
