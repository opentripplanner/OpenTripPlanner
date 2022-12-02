package org.opentripplanner.routing.api.request.request.filter;

import java.io.Serializable;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class FilterRequest implements Cloneable, Serializable, FilterPredicate {
  private SelectRequest include = new SelectRequest();
  private SelectRequest exclude = new SelectRequest();

  public SelectRequest getInclude() {
    return include;
  }

  public void setInclude(SelectRequest include) {
    this.include = include;
  }

  public SelectRequest getExclude() {
    return exclude;
  }

  public void setExclude(SelectRequest exclude) {
    this.exclude = exclude;
  }

  @Override
  public String toString() {
    return "FilterRequest{" +
      "include=" + include +
      ", exclude=" + exclude +
      '}';
  }

  @Override
  public FilterRequest clone() {
    try {
      var clone = (FilterRequest) super.clone();

      clone.include = this.include.clone();
      clone.exclude = this.exclude.clone();

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean routePredicate(Route route) {
    // skip subModes here, we are going to check it on trip level
    if (
      include.modes().isEmpty() &&
        exclude.modes().isEmpty() &&
        include.agencies().isEmpty() &&
        exclude.agencies().isEmpty() &&
        include.routes().isEmpty() &&
        exclude.routes().isEmpty() &&
        include.feeds().isEmpty() &&
        exclude.feeds().isEmpty()
    ) {
      // everything is empty. Do not have to check
      return true;
    }
    var allowedModes = include.modes();
    var bannedModes = exclude.modes();
    var allowedAgencies = include.agencies();
    var bannedAgencies = exclude.agencies();
    var allowedRoutes = include.routes();
    var bannedRoutes = exclude.routes();
    var allowedFeeds = include.feeds();
    var bannedFeeds = exclude.feeds();

    if (bannedModes.contains(route.getMode())) {
      return false;
    }

    if (bannedAgencies.contains(route.getAgency().getId())) {
      return false;
    }

    if (bannedRoutes.matches(route)) {
      return false;
    }

    if (bannedFeeds.contains(route.getId().getFeedId())) {
      return false;
    }

    boolean allowed = false;
    boolean allowListInUse = false;

    if (!allowedModes.isEmpty() ) {
      allowListInUse = true;
      if (allowedModes.contains(route.getMode())) {
        allowed = true;
      }
    }

    if (!allowedAgencies.isEmpty()) {
      allowListInUse = true;
      if (allowedAgencies.contains(route.getAgency().getId())) {
        allowed = true;
      }
    }

    if (!allowedRoutes.isEmpty()) {
      allowListInUse = true;
      if (allowedRoutes.matches(route)) {
        allowed = true;
      }
    }

    if (!allowedFeeds.isEmpty()) {
      allowListInUse = true;
      if (allowedFeeds.contains(route.getId().getFeedId())) {
        allowed = true;
      }
    }

    return !allowListInUse || allowed;
  }

  @Override
  public boolean tripTimesPredicate(TripTimes tripTimes) {
    // TODO: 2022-12-01 filters: this should be optimized
    var allowedModes = include.modes();
    var bannedModes = exclude.modes();
    var allowedSubModes = include.subModes();
    var bannedSubModes = exclude.subModes();
    var allowedAgencies = include.agencies();
    var bannedAgencies = exclude.agencies();
    var allowedRoutes = include.routes();
    var bannedRoutes = exclude.routes();
    var allowedFeeds = include.feeds();
    var bannedFeeds = exclude.feeds();
    var allowedTrips = include.trips();
    var bannedTrips = exclude.trips();

    var trip = tripTimes.getTrip();

    if (allowedTrips.isEmpty() &&
      bannedTrips.isEmpty() &&
      allowedSubModes.isEmpty() &&
      bannedSubModes.isEmpty()) {
      // no trip-specific filters specified
      // all irrelevant trips were already filtered out by routePredicate
      return true;
    }

    if (bannedModes.contains(trip.getMode())) {
      return false;
    }

    if (bannedSubModes.contains(trip.getNetexSubMode())) {
      return false;
    }

    if (bannedAgencies.contains(trip.getRoute().getAgency().getId())){
      return false;
    }

    if (bannedRoutes.matches(trip.getRoute())) {
      return false;
    }

    if (bannedFeeds.contains(trip.getId().getFeedId())) {
      return false;
    }

    if (bannedTrips.contains(trip.getId())) {
      return false;
    }

    boolean allowed = false;
    boolean allowListInUse = false;

    if (!allowedModes.isEmpty()) {
      allowListInUse = true;
      if (allowedModes.contains(trip.getMode())) {
        allowed = true;
      }
    }

    if (!allowedSubModes.isEmpty()) {
      allowListInUse = true;
      if (allowedSubModes.contains(trip.getNetexSubMode())) {
        allowed = true;
      }
    }

    if (!allowedAgencies.isEmpty()) {
      allowListInUse = true;
      if (allowedAgencies.contains(trip.getRoute().getAgency().getId())) {
        allowed = true;
      }
    }

    if (!allowedRoutes.isEmpty()) {
      allowListInUse = true;
      if (allowedRoutes.matches(trip.getRoute())) {
        allowed = true;
      }
    }

    if (!allowedFeeds.isEmpty()) {
      allowListInUse = true;
      if (allowedFeeds.contains(trip.getId().getFeedId())) {
        allowed = true;
      }
    }

    if (!allowedTrips.isEmpty()) {
      allowListInUse = true;
      if (allowedTrips.contains(trip.getId())) {
        allowed = true;
      }
    }

    return !allowListInUse || allowed;
  }
}
