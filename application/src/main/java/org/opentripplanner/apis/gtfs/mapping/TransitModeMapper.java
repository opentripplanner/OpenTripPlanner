package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * Maps transit mode from API to internal model.
 */
public class TransitModeMapper {

  public static TransitMode map(GraphQLTypes.GraphQLTransitMode mode) {
    return switch (mode) {
      case AIRPLANE -> TransitMode.AIRPLANE;
      case BUS -> TransitMode.BUS;
      case CABLE_CAR -> TransitMode.CABLE_CAR;
      case COACH -> TransitMode.COACH;
      case FERRY -> TransitMode.FERRY;
      case FUNICULAR -> TransitMode.FUNICULAR;
      case GONDOLA -> TransitMode.GONDOLA;
      case RAIL -> TransitMode.RAIL;
      case SUBWAY -> TransitMode.SUBWAY;
      case TRAM -> TransitMode.TRAM;
      case CARPOOL -> TransitMode.CARPOOL;
      case TAXI -> TransitMode.TAXI;
      case TROLLEYBUS -> TransitMode.TROLLEYBUS;
      case MONORAIL -> TransitMode.MONORAIL;
    };
  }
}
