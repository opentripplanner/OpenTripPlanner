package org.opentripplanner.apis.gtfs.mapping.routerequest;

import org.opentripplanner.apis.gtfs.generated.GraphQLTypes;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile;

/**
 * Maps ItineraryFilterDebugProfile from API to internal model.
 */
public class ItineraryFilterDebugProfileMapper {

  public static ItineraryFilterDebugProfile map(
    GraphQLTypes.GraphQLItineraryFilterDebugProfile profile
  ) {
    return switch (profile) {
      case LIMIT_TO_NUMBER_OF_ITINERARIES -> ItineraryFilterDebugProfile.LIMIT_TO_NUM_OF_ITINERARIES;
      case LIMIT_TO_SEARCH_WINDOW -> ItineraryFilterDebugProfile.LIMIT_TO_SEARCH_WINDOW;
      case LIST_ALL -> ItineraryFilterDebugProfile.LIST_ALL;
      case OFF -> ItineraryFilterDebugProfile.OFF;
    };
  }
}
