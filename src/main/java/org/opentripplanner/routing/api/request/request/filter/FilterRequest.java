package org.opentripplanner.routing.api.request.request.filter;

import java.io.Serializable;
import org.opentripplanner.model.modes.AllowTransitModeFilter;
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
    return "FilterRequest{" + "include=" + include + ", exclude=" + exclude + '}';
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
    // Route is accepted if it:
    //  matches with all allow-lists (empty list means match all)
    //  does not match with any ban-list

    var allowedModes = include.transportModes();
    var bannedModes = exclude.transportModes();
    var allowedAgencies = include.agencies();
    var bannedAgencies = exclude.agencies();
    var allowedRoutes = include.routes();
    var bannedRoutes = exclude.routes();
    var allowedFeeds = include.feeds();
    var bannedFeeds = exclude.feeds();

    if (
      allowedModes == null &&
        bannedModes == null &&
      allowedAgencies.isEmpty() &&
      bannedAgencies.isEmpty() &&
      allowedRoutes.isEmpty() &&
      bannedRoutes.isEmpty() &&
      allowedFeeds.isEmpty() &&
      bannedFeeds.isEmpty()
    ) {
      // everything is empty. Do not have to check
      return true;
    }

    if (
      bannedModes != null &&
        bannedModes.allows(route.getMode(), route.getNetexSubmode())
    ) {
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

    if (
      allowedModes != null &&
        allowedModes.allows(route.getMode(), route.getNetexSubmode())
    ) {
      return false;
    }

    if (!allowedAgencies.isEmpty() && !allowedAgencies.contains(route.getAgency().getId())) {
      return false;
    }

    if (!allowedRoutes.isEmpty() && !allowedRoutes.matches(route)) {
      return false;
    }

    if (!allowedFeeds.isEmpty() && !allowedFeeds.contains(route.getId().getFeedId())) {
      return false;
    }

    return true;
  }

  @Override
  public boolean tripTimesPredicate(TripTimes tripTimes) {
    // trip is accepted if it:
    //  matches with all allow-lists (empty list means match all)
    //  does not match with any ban-list

    // TODO: 2022-12-01 filters: this should be optimized
    var allowedModes = include.transportModes();
    var bannedModes = exclude.transportModes();
    var allowedAgencies = include.agencies();
    var bannedAgencies = exclude.agencies();
    var allowedRoutes = include.routes();
    var bannedRoutes = exclude.routes();
    var allowedFeeds = include.feeds();
    var bannedFeeds = exclude.feeds();
    var allowedTrips = include.trips();
    var bannedTrips = exclude.trips();

    var trip = tripTimes.getTrip();

    if (
      allowedTrips.isEmpty() &&
      bannedTrips.isEmpty() &&
      allowedModes == null &&
        bannedModes == null
    ) {
      // no trip-specific filters specified
      // all irrelevant trips were already filtered out by routePredicate
      return true;
    }

    if (
      bannedModes != null &&
      bannedModes.allows(trip.getMode(), trip.getNetexSubMode())
    ) {
      return false;
    }

    if (bannedAgencies.contains(trip.getRoute().getAgency().getId())) {
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

    if (
      allowedModes != null &&
      allowedModes.allows(trip.getMode(), trip.getNetexSubMode())
    ) {
      return false;
    }

    if (
      !allowedAgencies.isEmpty() && !allowedAgencies.contains(trip.getRoute().getAgency().getId())
    ) {
      return false;
    }

    if (!allowedRoutes.isEmpty() && !allowedRoutes.matches(trip.getRoute())) {
      return false;
    }

    if (!allowedFeeds.isEmpty() && !allowedFeeds.contains(trip.getId().getFeedId())) {
      return false;
    }

    if (!allowedTrips.isEmpty() && !allowedTrips.contains(trip.getId())) {
      return false;
    }

    return true;
  }
}
