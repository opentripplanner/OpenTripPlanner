package org.opentripplanner.routing.api.request.refactor.request;

import java.util.List;
import java.util.Set;
import org.opentripplanner.routing.api.request.DebugRaptor;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TransitRequest {
  // TODO: 2022-08-17 Should be a set?
  List<MainAndSubMode> modes = MainAndSubMode.all();
  Set<FeedScopedId> whiteListedAgencies = Set.of();
  Set<FeedScopedId> bannedAgencies = Set.of();
  Set<FeedScopedId> preferredAgencies = Set.of();
  Set<FeedScopedId> unpreferredAgencies = Set.of();
  RouteMatcher whiteListedRoutes = RouteMatcher.emptyMatcher();
  RouteMatcher preferredRoutes = RouteMatcher.emptyMatcher();
  RouteMatcher unpreferredRoutes = RouteMatcher.emptyMatcher();
  Set<FeedScopedId> bannedTrips = Set.of();
  DebugRaptor raptorDebugging = new DebugRaptor();
}
