package org.opentripplanner.routing.api.request.request.filter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.modes.AllowTransitModeFilter;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class SelectRequest implements Cloneable, Serializable {

  private AllowTransitModeFilter transportModes;
  private List<FeedScopedId> agencies = new ArrayList<>();
  private RouteMatcher routes = RouteMatcher.emptyMatcher();
  // TODO: 2022-11-29 group of routes
  private List<String> feeds = new ArrayList<>();

  public boolean matches(Route route) {
    if (
      this.transportModes != null &&
      !this.transportModes.match(route.getMode(), route.getNetexSubmode())
    ) {
      return false;
    }

    if (!agencies.isEmpty() && !agencies.contains(route.getAgency().getId())) {
      return false;
    }

    if (!routes.isEmpty() && !routes.matches(route)) {
      return false;
    }

    if (!feeds.isEmpty() && !feeds.contains(route.getId().getFeedId())) {
      return false;
    }

    return true;
  }

  public boolean matches(TripTimes tripTimes) {
    var trip = tripTimes.getTrip();

    if (
      this.transportModes != null &&
      !this.transportModes.match(trip.getMode(), trip.getNetexSubMode())
    ) {
      return false;
    }

    if (!agencies.isEmpty() && !agencies.contains(trip.getRoute().getAgency().getId())) {
      return false;
    }

    if (!feeds.isEmpty() && !feeds.contains(trip.getId().getFeedId())) {
      return false;
    }

    return true;
  }

  public AllowTransitModeFilter transportModes() {
    return transportModes;
  }

  public void setTransportModes(List<MainAndSubMode> transportModes) {
    if (!transportModes.isEmpty()) {
      this.transportModes = AllowTransitModeFilter.of(transportModes);
    }
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

  public List<String> feeds() {
    return feeds;
  }

  public void setFeeds(List<String> feeds) {
    this.feeds = feeds;
  }

  @Override
  public String toString() {
    return (
      "SelectRequest{" +
      "transportModes=" +
      transportModes +
      ", agencies=" +
      agencies +
      ", routes=" +
      routes +
      ", feeds=" +
      feeds +
      '}'
    );
  }

  @Override
  protected SelectRequest clone() throws CloneNotSupportedException {
    try {
      var clone = (SelectRequest) super.clone();

      // TODO: 2022-12-06 filters: check if we need to copy that
      clone.transportModes = this.transportModes;
      clone.agencies = List.copyOf(this.agencies);
      clone.routes = this.routes.clone();
      clone.feeds = List.copyOf(this.feeds);

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }
}
