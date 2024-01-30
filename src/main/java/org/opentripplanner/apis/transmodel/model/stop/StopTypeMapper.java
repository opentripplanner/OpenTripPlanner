package org.opentripplanner.apis.transmodel.model.stop;

import org.opentripplanner.transit.model.site.StopType;

/**
 * Maps the StopType enum to a String used in the GraphQL API.
 */
public class StopTypeMapper {

  public static String getStopType(StopType stopType) {
    return switch (stopType) {
      case REGULAR -> "regular";
      case FLEXIBLE_AREA -> "flexible_area";
      case FLEXIBLE_GROUP -> "flexible_group";
    };
  }
}
