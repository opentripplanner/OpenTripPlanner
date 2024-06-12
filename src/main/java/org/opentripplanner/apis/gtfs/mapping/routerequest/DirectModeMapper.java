package org.opentripplanner.apis.gtfs.mapping.routerequest;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.api.request.StreetMode;

/**
 * Maps direct street mode from API to internal model.
 */
public class DirectModeMapper {

  public static StreetMode map(GraphQLTypes.GraphQLPlanDirectMode mode) {
    return switch (mode) {
      case BICYCLE -> StreetMode.BIKE;
      case BICYCLE_RENTAL -> StreetMode.BIKE_RENTAL;
      case BICYCLE_PARKING -> StreetMode.BIKE_TO_PARK;
      case CAR -> StreetMode.CAR;
      case CAR_RENTAL -> StreetMode.CAR_RENTAL;
      case CAR_PARKING -> StreetMode.CAR_TO_PARK;
      case FLEX -> StreetMode.FLEXIBLE;
      case SCOOTER_RENTAL -> StreetMode.SCOOTER_RENTAL;
      case WALK -> StreetMode.WALK;
    };
  }
}
