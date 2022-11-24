package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FilterRequest implements Cloneable, Serializable {

  private List<MainAndSubMode> modes = MainAndSubMode.all();

  private List<FeedScopedId> whiteListedAgencies = List.of();
  private List<FeedScopedId> bannedAgencies = List.of();

  private RouteMatcher whiteListedRoutes = RouteMatcher.emptyMatcher();
  private RouteMatcher bannedRoutes = RouteMatcher.emptyMatcher();
  private List<FeedScopedId> whiteListedGroupsOfRoutes = List.of();
  private List<FeedScopedId> bannedGroupsOfRoutes = List.of();

  private List<FeedScopedId> bannedTrips = List.of();

  public void setModes(List<MainAndSubMode> modes) {
    this.modes = modes;
  }

  public List<MainAndSubMode> modes() {
    return modes;
  }

  public void setWhiteListedAgenciesFromSting(String s) {
    if (!s.isEmpty()) {
      whiteListedAgencies = FeedScopedId.parseListOfIds(s);
    }
  }

  /**
   * Only use certain named agencies
   */
  public void setWhiteListedAgencies(List<FeedScopedId> whiteListedAgencies) {
    this.whiteListedAgencies = whiteListedAgencies;
  }

  public List<FeedScopedId> whiteListedAgencies() {
    return whiteListedAgencies;
  }

  /**
   * Do not use certain named agencies
   */
  public void setBannedAgenciesFromSting(String s) {
    if (!s.isEmpty()) {
      bannedAgencies = FeedScopedId.parseListOfIds(s);
    }
  }

  /**
   * Do not use certain named agencies
   */
  public void setBannedAgencies(List<FeedScopedId> bannedAgencies) {
    this.bannedAgencies = bannedAgencies;
  }

  public List<FeedScopedId> bannedAgencies() {
    return bannedAgencies;
  }

  /**
   * Only use certain named routes
   */
  public void setWhiteListedRoutesFromString(String s) {
    if (!s.isEmpty()) {
      whiteListedRoutes = RouteMatcher.parse(s);
    } else {
      whiteListedRoutes = RouteMatcher.emptyMatcher();
    }
  }

  /**
   * Only use certain named routes
   */
  public void setWhiteListedRoutes(RouteMatcher whiteListedRoutes) {
    this.whiteListedRoutes = whiteListedRoutes;
  }

  /**
   * Only use certain named routes
   */
  public RouteMatcher whiteListedRoutes() {
    return whiteListedRoutes;
  }

  /**
   * Do not use certain named routes. The paramter format is: feedId_routeId,feedId_routeId,feedId_routeId
   * This parameter format is completely nonstandard and should be revised for the 2.0 API, see
   * issue #1671.
   */
  public void setBannedRoutesFromString(String s) {
    if (!s.isEmpty()) {
      bannedRoutes = RouteMatcher.parse(s);
    } else {
      bannedRoutes = RouteMatcher.emptyMatcher();
    }
  }

  /**
   * Do not use certain named routes. The paramter format is: feedId_routeId,feedId_routeId,feedId_routeId
   * This parameter format is completely nonstandard and should be revised for the 2.0 API, see
   * issue #1671.
   */
  public void setBannedRoutes(RouteMatcher bannedRoutes) {
    this.bannedRoutes = bannedRoutes;
  }

  public RouteMatcher bannedRoutes() {
    return bannedRoutes;
  }

  /**
   * Do not use certain trips
   */
  public void setBannedTripsFromString(String ids) {
    if (!ids.isEmpty()) {
      bannedTrips = FeedScopedId.parseListOfIds(ids);
    }
  }

  /**
   * Do not use certain trips
   */
  public void setBannedTrips(List<FeedScopedId> bannedTrips) {
    this.bannedTrips = bannedTrips;
  }

  public List<FeedScopedId> bannedTrips() {
    return bannedTrips;
  }

  public void setWhiteListedGroupsOfRoutes(List<FeedScopedId> whiteListedGroupsOfRoutes) {
    this.whiteListedGroupsOfRoutes = whiteListedGroupsOfRoutes;
  }

  public List<FeedScopedId> whiteListedGroupsOfRoutes() {
    return whiteListedGroupsOfRoutes;
  }

  public void setBannedGroupsOfRoutes(List<FeedScopedId> bannedGroupsOfRoutes) {
    this.bannedGroupsOfRoutes = bannedGroupsOfRoutes;
  }

  public List<FeedScopedId> bannedGroupsOfRoutes() {
    return bannedGroupsOfRoutes;
  }

  public FilterRequest clone() {
    try {
      var clone = (FilterRequest) super.clone();

      clone.modes = new ArrayList<>(this.modes);
      clone.whiteListedAgencies = List.copyOf(this.whiteListedAgencies);
      clone.bannedAgencies = List.copyOf(this.bannedAgencies);
      clone.whiteListedRoutes = this.whiteListedRoutes.clone();
      clone.bannedRoutes = this.bannedRoutes.clone();
      clone.bannedTrips = List.copyOf(this.bannedTrips);

      return clone;
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    return (
      "FilterRequest{" +
      "modes=" +
      modes +
      ", whiteListedAgencies=" +
      whiteListedAgencies +
      ", bannedAgencies=" +
      bannedAgencies +
      ", whiteListedRoutes=" +
      whiteListedRoutes +
      ", bannedRoutes=" +
      bannedRoutes +
      ", whiteListedGroupsOfRoutes=" +
      whiteListedGroupsOfRoutes +
      ", bannedGroupsOfRoutes=" +
      bannedGroupsOfRoutes +
      ", bannedTrips=" +
      bannedTrips +
      '}'
    );
  }
}
