package org.opentripplanner.routing.api.request.request.filter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class SelectRequest implements Cloneable, Serializable {
  private List<MainAndSubMode> transportModes = new ArrayList<>();
  private List<FeedScopedId> agencies = new ArrayList<>();
  private RouteMatcher routes = RouteMatcher.emptyMatcher();
  // TODO: 2022-11-29 group of routes
  private List<FeedScopedId> trips = new ArrayList<>();
  private List<String> feeds = new ArrayList<>();

  public List<MainAndSubMode> transportModes() {
    return transportModes;
  }

  public void setTransportModes(List<MainAndSubMode> transportModes) {
    this.transportModes = transportModes;
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

  @Override
  public String toString() {
    return "SelectRequest{" +
      "transportModes=" + transportModes +
      ", agencies=" + agencies +
      ", routes=" + routes +
      ", trips=" + trips +
      ", feeds=" + feeds +
      '}';
  }

  @Override
  protected SelectRequest clone() throws CloneNotSupportedException {
    try {
      var clone = (SelectRequest) super.clone();

      clone.transportModes = List.copyOf(this.transportModes);
      clone.agencies = List.copyOf(this.agencies);
      clone.routes = this.routes.clone();
      clone.trips = List.copyOf(this.trips);
      clone.feeds = List.copyOf(this.feeds);

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
