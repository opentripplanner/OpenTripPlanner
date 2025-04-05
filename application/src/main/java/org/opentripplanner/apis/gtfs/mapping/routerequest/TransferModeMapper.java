package org.opentripplanner.apis.gtfs.mapping.routerequest;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.api.request.StreetMode;

/**
 * Maps transfer street mode from API to internal model or vice versa.
 */
public class TransferModeMapper {

  public static StreetMode map(GraphQLTypes.GraphQLPlanTransferMode mode) {
    return switch (mode) {
      case CAR -> StreetMode.CAR;
      case BICYCLE -> StreetMode.BIKE;
      case WALK -> StreetMode.WALK;
    };
  }

  public static GraphQLTypes.GraphQLPlanTransferMode map(StreetMode mode) {
    return switch (mode) {
      case BIKE -> GraphQLTypes.GraphQLPlanTransferMode.BICYCLE;
      case CAR -> GraphQLTypes.GraphQLPlanTransferMode.CAR;
      case WALK,
        BIKE_RENTAL,
        CAR_HAILING,
        CAR_RENTAL,
        CAR_PICKUP,
        CAR_TO_PARK,
        BIKE_TO_PARK,
        FLEXIBLE,
        SCOOTER_RENTAL,
        NOT_SET -> GraphQLTypes.GraphQLPlanTransferMode.WALK;
    };
  }
}
