package org.opentripplanner.apis.gtfs.mapping;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLBikesAllowed;
import org.opentripplanner.transit.model.network.BikeAccess;

public class BikesAllowedMapper {

  public static GraphQLBikesAllowed map(BikeAccess bikesAllowed) {
    return switch (bikesAllowed) {
      case UNKNOWN -> GraphQLBikesAllowed.NO_INFORMATION;
      case ALLOWED -> GraphQLBikesAllowed.ALLOWED;
      case NOT_ALLOWED -> GraphQLBikesAllowed.NOT_ALLOWED;
    };
  }
}
