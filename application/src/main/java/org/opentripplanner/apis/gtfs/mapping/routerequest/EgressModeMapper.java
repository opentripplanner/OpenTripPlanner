package org.opentripplanner.apis.gtfs.mapping.routerequest;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.api.request.StreetMode;

/**
 * Maps egress street mode from API to internal model or vice versa.
 */
public class EgressModeMapper {

  public static StreetMode map(GraphQLTypes.GraphQLPlanEgressMode mode) {
    return switch (mode) {
      case BICYCLE -> StreetMode.BIKE;
      case BICYCLE_RENTAL -> StreetMode.BIKE_RENTAL;
      case CAR -> StreetMode.CAR;
      case CAR_RENTAL -> StreetMode.CAR_RENTAL;
      case CAR_PICKUP -> StreetMode.CAR_PICKUP;
      case FLEX -> StreetMode.FLEXIBLE;
      case SCOOTER_RENTAL -> StreetMode.SCOOTER_RENTAL;
      case WALK -> StreetMode.WALK;
    };
  }

  public static GraphQLTypes.GraphQLPlanEgressMode map(StreetMode mode) {
    return switch (mode) {
      case BIKE -> GraphQLTypes.GraphQLPlanEgressMode.BICYCLE;
      case BIKE_RENTAL -> GraphQLTypes.GraphQLPlanEgressMode.BICYCLE_RENTAL;
      case CAR -> GraphQLTypes.GraphQLPlanEgressMode.CAR;
      case CAR_RENTAL -> GraphQLTypes.GraphQLPlanEgressMode.CAR_RENTAL;
      case CAR_PICKUP -> GraphQLTypes.GraphQLPlanEgressMode.CAR_PICKUP;
      case FLEXIBLE -> GraphQLTypes.GraphQLPlanEgressMode.FLEX;
      case SCOOTER_RENTAL -> GraphQLTypes.GraphQLPlanEgressMode.SCOOTER_RENTAL;
      case WALK,
        CAR_HAILING,
        CAR_TO_PARK,
        BIKE_TO_PARK,
        NOT_SET -> GraphQLTypes.GraphQLPlanEgressMode.WALK;
    };
  }
}
