package org.opentripplanner.ext.restapi.mapping;

import org.opentripplanner.transit.model.network.BikeAccess;

public class BikeAccessMapper {

  public static int mapToApi(BikeAccess bikeAccess) {
    if (bikeAccess == null) {
      return 0;
    }
    return switch (bikeAccess) {
      case ALLOWED -> 1;
      case NOT_ALLOWED -> 2;
      default -> 0;
    };
  }
}
