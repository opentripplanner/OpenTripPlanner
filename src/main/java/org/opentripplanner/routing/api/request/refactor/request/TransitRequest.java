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

  public void setModes(List<MainAndSubMode> modes) {
    this.modes = modes;
  }

  public List<MainAndSubMode> modes() {
    return modes;
  }

  public void setWhiteListedAgenciesFromSting(String s) {
    if (!s.isEmpty()) {
      whiteListedAgencies = FeedScopedId.parseSetOfIds(s);
    }
  }

  public void setWhiteListedAgencies(Set<FeedScopedId> whiteListedAgencies) {
    this.whiteListedAgencies = whiteListedAgencies;
  }

  public Set<FeedScopedId> whiteListedAgencies() {
    return whiteListedAgencies;
  }

  public void setBannedAgenciesFromSting(String s) {
    if (!s.isEmpty()) {
      bannedAgencies = FeedScopedId.parseSetOfIds(s);
    }
  }

  public void setBannedAgencies(Set<FeedScopedId> bannedAgencies) {
    this.bannedAgencies = bannedAgencies;
  }

  public Set<FeedScopedId> bannedAgencies() {
    return bannedAgencies;
  }

  public void setPreferredAgenciesFromString(String s) {
    if (!s.isEmpty()) {
      preferredAgencies = FeedScopedId.parseSetOfIds(s);
    }
  }

  public void setPreferredAgencies(Set<FeedScopedId> preferredAgencies) {
    this.preferredAgencies = preferredAgencies;
  }

  public Set<FeedScopedId> preferredAgencies() {
    return preferredAgencies;
  }

  public void setUnpreferredAgenciesFromString(String s) {
    if (!s.isEmpty()) {
      unpreferredAgencies = FeedScopedId.parseSetOfIds(s);
    }
  }

  public void setUnpreferredAgencies(Set<FeedScopedId> unpreferredAgencies) {
    this.unpreferredAgencies = unpreferredAgencies;
  }

  public Set<FeedScopedId> unpreferredAgencies() {
    return unpreferredAgencies;
  }

  public void setWhiteListedRoutesFromString(String s) {
    if (!s.isEmpty()) {
      whiteListedRoutes = RouteMatcher.parse(s);
    } else {
      whiteListedRoutes = RouteMatcher.emptyMatcher();
    }
  }

  public void setWhiteListedRoutes(RouteMatcher whiteListedRoutes) {
    this.whiteListedRoutes = whiteListedRoutes;
  }

  public RouteMatcher whiteListedRoutes() {
    return whiteListedRoutes;
  }

  public void setBannedRoutesFromString(String s) {
    if (!s.isEmpty()) {
      bannedRoutes = RouteMatcher.parse(s);
    } else {
      bannedRoutes = RouteMatcher.emptyMatcher();
    }
  }

  public void setBannedRoutes(RouteMatcher bannedRoutes) {
    this.bannedRoutes = bannedRoutes;
  }

  public RouteMatcher bannedRoutes() {
    return bannedRoutes;
  }

  public void setPreferredRoutesFromString(String s) {
    if (!s.isEmpty()) {
      preferredRoutes = RouteMatcher.parse(s);
    } else {
      preferredRoutes = RouteMatcher.emptyMatcher();
    }
  }

  public void setPreferredRoutes(RouteMatcher preferredRoutes) {
    this.preferredRoutes = preferredRoutes;
  }

  public RouteMatcher preferredRoutes() {
    return preferredRoutes;
  }

  public void setUnpreferredRoutesFromString(String s) {
    if (!s.isEmpty()) {
      unpreferredRoutes = RouteMatcher.parse(s);
    } else {
      unpreferredRoutes = RouteMatcher.emptyMatcher();
    }
  }

  public void setUnpreferredRoutes(RouteMatcher unpreferredRoutes) {
    this.unpreferredRoutes = unpreferredRoutes;
  }

  public RouteMatcher unpreferredRoutes() {
    return unpreferredRoutes;
  }

  public void setBannedTripsFromString(String ids) {
    if (!ids.isEmpty()) {
      bannedTrips = FeedScopedId.parseSetOfIds(ids);
    }
  }

  public void setBannedTrips(Set<FeedScopedId> bannedTrips) {
    this.bannedTrips = bannedTrips;
  }

  public Set<FeedScopedId> bannedTrips() {
    return bannedTrips;
  }

  public void setRaptorDebugging(DebugRaptor raptorDebugging) {
    this.raptorDebugging = raptorDebugging;
  }

  public DebugRaptor raptorDebugging() {
    return raptorDebugging;
  }
}
