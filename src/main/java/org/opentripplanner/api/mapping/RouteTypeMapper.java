package org.opentripplanner.api.mapping;

import org.opentripplanner.model.TransitMode;

public class RouteTypeMapper {

  private static final int DEFAULT_ROUTE_TYPE = -1;

  public static int mapToApi(TransitMode domain) {
    if(domain == null) { return DEFAULT_ROUTE_TYPE; }

    switch (domain) {
      case RAIL:
        return 2;
      case COACH:
      case BUS:
        return 3;
      case SUBWAY:
        return 1;
      case TRAM:
        return 0;
      case FERRY:
        return 4;
      case CABLE_CAR:
        return 5;
      case GONDOLA:
        return 6;
      case FUNICULAR:
        return 7;
      case TROLLEYBUS:
        return 11;
      case MONORAIL:
        return 12;
      case AIRPLANE:
      default:
        return DEFAULT_ROUTE_TYPE;
    }
  }
}
