package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * Maps transit mode from API to internal model or vice versa.
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

  public static GraphQLTypes.GraphQLTransitMode map(TransitMode mode) {
    return switch (mode) {
      case AIRPLANE -> GraphQLTypes.GraphQLTransitMode.AIRPLANE;
      case BUS -> GraphQLTypes.GraphQLTransitMode.BUS;
      case CABLE_CAR -> GraphQLTypes.GraphQLTransitMode.CABLE_CAR;
      case COACH -> GraphQLTypes.GraphQLTransitMode.COACH;
      case FERRY -> GraphQLTypes.GraphQLTransitMode.FERRY;
      case FUNICULAR -> GraphQLTypes.GraphQLTransitMode.FUNICULAR;
      case GONDOLA -> GraphQLTypes.GraphQLTransitMode.GONDOLA;
      case RAIL -> GraphQLTypes.GraphQLTransitMode.RAIL;
      case SUBWAY -> GraphQLTypes.GraphQLTransitMode.SUBWAY;
      case TRAM -> GraphQLTypes.GraphQLTransitMode.TRAM;
      case CARPOOL -> GraphQLTypes.GraphQLTransitMode.CARPOOL;
      case TAXI -> GraphQLTypes.GraphQLTransitMode.TAXI;
      case TROLLEYBUS -> GraphQLTypes.GraphQLTransitMode.TROLLEYBUS;
      case MONORAIL -> GraphQLTypes.GraphQLTransitMode.MONORAIL;
    };
  }
}
