package org.opentripplanner.routing.algorithm.filterchain.filter;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.ItineraryDeletionFlagger;

/**
 * This class is responsible for flagging itineraries for deletion based on a predicate in the
 * supplied ItineraryDeletionFlagger. The itineraries are not actually deleted at this point, just
 * flagged. They are typically deleted later if debug mode is disabled.
 */
public class DeletionFlaggingFilter implements ItineraryListFilter {

  private final ItineraryDeletionFlagger flagger;

  public DeletionFlaggingFilter(ItineraryDeletionFlagger flagger) {
    this.flagger = flagger;
  }

  public String name() {
    return flagger.name();
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    List<Itinerary> filterInput;
    if (flagger.skipAlreadyFlaggedItineraries()) {
      filterInput =
        itineraries
          .stream()
          .filter(Predicate.not(Itinerary::isFlaggedForDeletion))
          .collect(Collectors.toList());
    } else {
      filterInput = itineraries;
    }

    for (Itinerary it : flagger.flagForRemoval(filterInput)) {
      it.flagForDeletion(
        new SystemNotice(
          flagger.name(),
          "This itinerary is marked as deleted by the " + flagger.name() + " filter."
        )
      );
    }

    return itineraries;
  }
}
