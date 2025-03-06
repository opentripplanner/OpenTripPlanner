package org.opentripplanner.apis.gtfs.mapping.routerequest;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.api.request.StreetMode;

/**
 * Maps access street mode from API to internal model or vice versa.
 */
public class AccessModeMapper {

  public static StreetMode map(GraphQLTypes.GraphQLPlanAccessMode mode) {
    return switch (mode) {
      case BICYCLE -> StreetMode.BIKE;
      case BICYCLE_RENTAL -> StreetMode.BIKE_RENTAL;
      case BICYCLE_PARKING -> StreetMode.BIKE_TO_PARK;
      case CAR -> StreetMode.CAR;
      case CAR_RENTAL -> StreetMode.CAR_RENTAL;
      case CAR_PARKING -> StreetMode.CAR_TO_PARK;
      case CAR_DROP_OFF -> StreetMode.CAR_PICKUP;
      case FLEX -> StreetMode.FLEXIBLE;
      case SCOOTER_RENTAL -> StreetMode.SCOOTER_RENTAL;
      case WALK -> StreetMode.WALK;
    };
  }

  public static GraphQLTypes.GraphQLPlanAccessMode map(StreetMode mode) {
    return switch (mode) {
      case BIKE -> GraphQLTypes.GraphQLPlanAccessMode.BICYCLE;
      case BIKE_RENTAL -> GraphQLTypes.GraphQLPlanAccessMode.BICYCLE_RENTAL;
      case BIKE_TO_PARK -> GraphQLTypes.GraphQLPlanAccessMode.BICYCLE_PARKING;
      case CAR -> GraphQLTypes.GraphQLPlanAccessMode.CAR;
      case CAR_RENTAL -> GraphQLTypes.GraphQLPlanAccessMode.CAR_RENTAL;
      case CAR_TO_PARK -> GraphQLTypes.GraphQLPlanAccessMode.CAR_PARKING;
      case CAR_PICKUP -> GraphQLTypes.GraphQLPlanAccessMode.CAR_DROP_OFF;
      case FLEXIBLE -> GraphQLTypes.GraphQLPlanAccessMode.FLEX;
      case SCOOTER_RENTAL -> GraphQLTypes.GraphQLPlanAccessMode.SCOOTER_RENTAL;
      case WALK, CAR_HAILING, NOT_SET -> GraphQLTypes.GraphQLPlanAccessMode.WALK;
    };
  }
}
