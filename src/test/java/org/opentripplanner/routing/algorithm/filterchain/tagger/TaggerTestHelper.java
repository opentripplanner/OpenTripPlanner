package org.opentripplanner.routing.algorithm.filterchain.tagger;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;

public class TaggerTestHelper {

    protected static List<Itinerary> process(List<Itinerary> itineraries, ItineraryTagger filter) {
        List<Itinerary> filtered = filter.getTaggedItineraries(itineraries);
        return itineraries.stream()
                .filter(Predicate.not(filtered::contains))
                .collect(Collectors.toList());
    }
}
