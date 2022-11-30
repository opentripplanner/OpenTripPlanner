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

  public List<TransitMode> getModes() {
    return modes;
  }

  public void setModes(List<TransitMode> modes) {
    this.modes = modes;
  }

  public List<SubMode> getSubModes() {
    return subModes;
  }

  public void setSubModes(List<SubMode> subModes) {
    this.subModes = subModes;
  }

  public void setAgencies(List<FeedScopedId> agencies) {
    this.agencies = agencies;
  }

  public void setAgenciesFromString(String s) {
    if (!s.isEmpty()) {
      this.agencies = FeedScopedId.parseListOfIds(s);
    }
  }

  public List<FeedScopedId> agencies() {
    return agencies;
  }

  public void setRoutesFromString(String s) {
    if (!s.isEmpty()) {
      this.routes = RouteMatcher.parse(s);
    } else {
      this.routes = RouteMatcher.emptyMatcher();
    }
  }

  public void setRoutes(RouteMatcher routes) {
    this.routes = routes;
  }

  public RouteMatcher routes() {
    return this.routes;
  }

  public void setTripsFromString(String ids) {
    if (!ids.isEmpty()) {
      this.trips = FeedScopedId.parseListOfIds(ids);
    }
  }

  public void setTrips(List<FeedScopedId> trips) {
    this.trips = trips;
  }

  public List<FeedScopedId> trips() {
    return trips;
  }

  public List<String> feeds() {
    return feeds;
  }

  public void setFeeds(List<String> feeds) {
    this.feeds = feeds;
  }
}
