package org.opentripplanner.routing.api.request.refactor.request;

import java.util.List;
import java.util.Set;
import org.opentripplanner.routing.api.request.DebugRaptor;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TransitRequest {
  // TODO: 2022-08-17 Should be a set?
  private List<MainAndSubMode> modes = MainAndSubMode.all();
  private Set<FeedScopedId> whiteListedAgencies = Set.of();
  private Set<FeedScopedId> bannedAgencies = Set.of();
  private Set<FeedScopedId> preferredAgencies = Set.of();
  private Set<FeedScopedId> unpreferredAgencies = Set.of();
  private RouteMatcher whiteListedRoutes = RouteMatcher.emptyMatcher();
  private RouteMatcher preferredRoutes = RouteMatcher.emptyMatcher();
  private RouteMatcher unpreferredRoutes = RouteMatcher.emptyMatcher();
  private Set<FeedScopedId> bannedTrips = Set.of();
  private DebugRaptor raptorDebugging = new DebugRaptor();

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
