package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;

public class DeletionFlaggerTestHelper {

  protected static List<Itinerary> process(
    List<Itinerary> itineraries,
    ItineraryDeletionFlagger flagger
  ) {
    List<Itinerary> filtered = flagger.flagForRemoval(itineraries);
    return itineraries
      .stream()
      .filter(Predicate.not(filtered::contains))
      .collect(Collectors.toList());
  }
}
