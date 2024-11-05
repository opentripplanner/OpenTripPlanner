package org.opentripplanner.apis.gtfs.mapping.routerequest;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.api.request.StreetMode;

/**
 * Maps egress street mode from API to internal model.
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
}
