package org.opentripplanner.routing.api.request.request;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class SelectRequest {
  private List<TransitMode> modes = new ArrayList<>();
  private List<SubMode> subModes = new ArrayList<>();
  private List<FeedScopedId> agencies = new ArrayList<>();
  private RouteMatcher routes = RouteMatcher.emptyMatcher();
  // TODO: 2022-11-29 group of routes
  private List<FeedScopedId> trips = new ArrayList<>();
  private List<String> feeds = new ArrayList<>();
}
