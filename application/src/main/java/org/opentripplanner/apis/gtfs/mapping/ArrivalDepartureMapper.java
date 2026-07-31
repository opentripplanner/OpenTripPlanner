package org.opentripplanner.apis.gtfs.mapping;

import javax.annotation.Nullable;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLArrivalDeparture;
import org.opentripplanner.transit.service.ArrivalDeparture;

/**
 * Maps the GraphQL API's {@code ArrivalDeparture} enum onto the internal {@link ArrivalDeparture}.
 */
public final class ArrivalDepartureMapper {

  /**
   * Maps the API enum to the internal one. If no value is given, calls that either allow pickup
   * or drop off are included by returning {@link ArrivalDeparture#BOTH}.
   */
  public static ArrivalDeparture map(@Nullable GraphQLArrivalDeparture arrivalDeparture) {
    if (arrivalDeparture == null) {
      return ArrivalDeparture.BOTH;
    }
    return switch (arrivalDeparture) {
      case ARRIVALS -> ArrivalDeparture.ARRIVALS;
      case EITHER -> ArrivalDeparture.BOTH;
      case DEPARTURES -> ArrivalDeparture.DEPARTURES;
    };
  }
}
