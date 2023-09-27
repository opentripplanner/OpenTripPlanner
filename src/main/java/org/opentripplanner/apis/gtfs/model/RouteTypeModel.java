package org.opentripplanner.apis.gtfs.model;

import org.opentripplanner.transit.model.organization.Agency;

/**
 * Class for route types. If agency is defined, the object is the route for the specific agency.
 */
public class RouteTypeModel {

  /**
   * If defined, this is the route type is only relevant for the agency.
   */
  private final Agency agency;

  /**
   * Route type (GTFS).
   */
  private final int routeType;

  /**
   * Route type only covers routes of this feed.
   */
  private final String feedId;

  public RouteTypeModel(Agency agency, int routeType, String feedId) {
    this.agency = agency;
    this.routeType = routeType;
    this.feedId = feedId;
  }

  public Agency getAgency() {
    return agency;
  }

  public int getRouteType() {
    return routeType;
  }

  public String getFeedId() {
    return feedId;
  }
}
