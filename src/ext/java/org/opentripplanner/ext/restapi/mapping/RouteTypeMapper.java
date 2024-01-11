package org.opentripplanner.ext.restapi.mapping;

import org.opentripplanner.transit.model.basic.TransitMode;

public class RouteTypeMapper {

  private static final int DEFAULT_ROUTE_TYPE = -1;

  public static int mapToApi(TransitMode domain) {
    if (domain == null) {
      return DEFAULT_ROUTE_TYPE;
    }

    return switch (domain) {
      case RAIL -> 2;
      case COACH, BUS -> 3;
      case SUBWAY -> 1;
      case TRAM -> 0;
      case FERRY -> 4;
      case AIRPLANE -> 1100;
      case CABLE_CAR -> 5;
      case GONDOLA -> 6;
      case FUNICULAR -> 7;
      case TROLLEYBUS -> 11;
      case MONORAIL -> 12;
      case TAXI -> 1500;
      case CARPOOL -> 1551;
    };
  }
}
