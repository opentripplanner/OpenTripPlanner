package org.opentripplanner.routing.api.request.request.filter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.modes.AllowTransitModeFilter;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class SelectRequest implements Serializable {

  public static Builder of() {
    return new Builder();
  }

  private final List<MainAndSubMode> transportModes;
  private final AllowTransitModeFilter transportModeFilter;
  private final List<FeedScopedId> agencies;
  private final List<FeedScopedId> groupOfRoutes;
  private final RouteMatcher routes;

  public SelectRequest(Builder builder) {
    if (builder.transportModes.isEmpty()) {
      this.transportModeFilter = null;
    } else {
      this.transportModeFilter = AllowTransitModeFilter.of(builder.transportModes);
    }

    // TODO: 2022-12-20 filters: having list of modes and modes filter in same instance is not very elegant
    this.transportModes = builder.transportModes;

    this.agencies = List.copyOf(builder.agencies);
    this.groupOfRoutes = List.copyOf(builder.groupOfRoutes);
    this.routes = builder.routes;
  }

  public boolean matches(Route route) {
    if (
      this.transportModeFilter != null &&
      !this.transportModeFilter.match(route.getMode(), route.getNetexSubmode())
    ) {
      return false;
    }

    if (!agencies.isEmpty() && !agencies.contains(route.getAgency().getId())) {
      return false;
    }

    if (!routes.isEmpty() && !routes.matches(route)) {
      return false;
    }

    if (!groupOfRoutes.isEmpty()) {
      var ids = new ArrayList<FeedScopedId>();
      for (var gor : route.getGroupsOfRoutes()) {
        ids.add(gor.getId());
      }

      if (Collections.disjoint(groupOfRoutes, ids)) {
        return false;
      }
    }

    return true;
  }

  public boolean matches(TripTimes tripTimes) {
    var trip = tripTimes.getTrip();

    if (
      this.transportModeFilter != null &&
      !this.transportModeFilter.match(trip.getMode(), trip.getNetexSubMode())
    ) {
      return false;
    }

    if (!agencies.isEmpty() && !agencies.contains(trip.getRoute().getAgency().getId())) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(SelectRequest.class)
      .addObj("transportModes", transportModesToString(), null)
      .addCol("agencies", agencies, List.of())
      .addObj("routes", routes, RouteMatcher.emptyMatcher())
      .toString();
  }

  public List<MainAndSubMode> transportModes() {
    return transportModes;
  }

  public AllowTransitModeFilter transportModeFilter() {
    return transportModeFilter;
  }

  public List<FeedScopedId> agencies() {
    return agencies;
  }

  public RouteMatcher routes() {
    return this.routes;
  }

  private String transportModesToString() {
    if (transportModes == null) {
      return null;
    }
    if (transportModes.stream().allMatch(MainAndSubMode::isMainModeOnly)) {
      int size = transportModes.size();
      int total = MainAndSubMode.all().size();
      if (size == total) {
        return "ALL-MAIN-MODES";
      }
      if (size + 3 >= total) {
        // If 3 or less of the main modes are *excluded* we guess that the user did exclude, and
        // not included everything. This make it much easier to read: "NOT [FERRY]" instead of
        // "[AIRPLANE, CABLE_CAR, CARPOOL, COACH ... "
        return "NOT " + MainAndSubMode.toString(MainAndSubMode.notMainModes(transportModes));
      }
    }
    return MainAndSubMode.toString(transportModes);
  }

  public static class Builder {

    private List<MainAndSubMode> transportModes = new ArrayList<>();
    private List<FeedScopedId> agencies = new ArrayList<>();
    private List<FeedScopedId> groupOfRoutes = new ArrayList<>();
    private RouteMatcher routes = RouteMatcher.emptyMatcher();

    public Builder withTransportModes(List<MainAndSubMode> transportModes) {
      this.transportModes = transportModes;
      return this;
    }

    public Builder addTransportMode(MainAndSubMode transportMode) {
      this.transportModes.add(transportMode);
      return this;
    }

    public Builder withAgenciesFromString(String s) {
      if (!s.isEmpty()) {
        this.agencies = FeedScopedId.parseListOfIds(s);
      }
      return this;
    }

    public Builder withAgencies(List<FeedScopedId> agencies) {
      this.agencies = agencies;
      return this;
    }

    public Builder withRoutesFromString(String s) {
      if (!s.isEmpty()) {
        this.routes = RouteMatcher.parse(s);
      } else {
        this.routes = RouteMatcher.emptyMatcher();
      }
      return this;
    }

    public Builder withRoutes(RouteMatcher routes) {
      this.routes = routes;
      return this;
    }

    public Builder withGroupOfRoutes(List<FeedScopedId> groupOfRoutes) {
      this.groupOfRoutes = groupOfRoutes;
      return this;
    }

    public SelectRequest build() {
      return new SelectRequest(this);
    }
  }
}
