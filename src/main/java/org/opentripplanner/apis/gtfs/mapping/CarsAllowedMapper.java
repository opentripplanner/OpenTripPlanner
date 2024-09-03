package org.opentripplanner.apis.gtfs.mapping;

import javax.annotation.Nonnull;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLCarsAllowed;
import org.opentripplanner.transit.model.network.CarAccess;

public class CarsAllowedMapper {

  @Nonnull
  public static GraphQLCarsAllowed map(@Nonnull CarAccess carsAllowed) {
    return switch (carsAllowed) {
      case UNKNOWN -> GraphQLCarsAllowed.NO_INFORMATION;
      case ALLOWED -> GraphQLCarsAllowed.ALLOWED;
      case NOT_ALLOWED -> GraphQLCarsAllowed.NOT_ALLOWED;
    };
  }
}
