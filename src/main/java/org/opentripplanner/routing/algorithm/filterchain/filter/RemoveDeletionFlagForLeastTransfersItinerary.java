package org.opentripplanner.routing.algorithm.filterchain.filter;

import static org.opentripplanner.routing.algorithm.filterchain.comparator.SortOrderComparator.numberOfTransfersComparator;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

/**
 * This filter makes sure that the itinerary with the least amount of transfers is not marked for
 * deletion
 */
public class RemoveDeletionFlagForLeastTransfersItinerary implements ItineraryListFilter {

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        itineraries.stream()
                .min(numberOfTransfersComparator())
                .ifPresent(itinerary -> itinerary.systemNotices.clear());

        return itineraries;
    }
}
