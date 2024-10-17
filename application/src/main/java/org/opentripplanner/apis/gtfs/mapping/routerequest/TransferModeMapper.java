package org.opentripplanner.apis.gtfs.mapping.routerequest;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.api.request.StreetMode;

/**
 * Maps transfer street mode from API to internal model.
 */
public class TransferModeMapper {

  public static StreetMode map(GraphQLTypes.GraphQLPlanTransferMode mode) {
    return switch (mode) {
      case CAR -> StreetMode.CAR;
      case BICYCLE -> StreetMode.BIKE;
      case WALK -> StreetMode.WALK;
    };
  }
}
