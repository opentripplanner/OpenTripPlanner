package org.opentripplanner.routing.algorithm.filterchain.framework.filterchain;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.filters.system.OutsideSearchWindowFilter;
import org.opentripplanner.routing.api.request.preference.ItineraryFilterDebugProfile;

/**
 * This class will remove itineraries from the list which are flagged for deletion by the
 * filters.
 */
public class DeleteResultHandler {

  private final ItineraryFilterDebugProfile debug;
  private final int numOfItineraries;

  public DeleteResultHandler(ItineraryFilterDebugProfile debug, int numOfItineraries) {
    this.debug = debug;
    this.numOfItineraries = numOfItineraries;
  }

  public List<Itinerary> filter(List<Itinerary> itineraries) {
    return switch (debug) {
      case OFF -> itineraries
        .stream()
        .filter(Predicate.not(Itinerary::isFlaggedForDeletion))
        .collect(Collectors.toList());
      case LIST_ALL -> itineraries;
      case LIMIT_TO_NUM_OF_ITINERARIES -> itineraries
        .stream()
        .limit(numOfItineraries)
        .collect(Collectors.toList());
      case LIMIT_TO_SEARCH_WINDOW -> itineraries
        .stream()
        .filter(Predicate.not(OutsideSearchWindowFilter::taggedBy))
        .collect(Collectors.toList());
    };
  }
}
