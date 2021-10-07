package org.opentripplanner.routing.algorithm.filterchain.filter;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilterChainBuilder;
import org.opentripplanner.routing.algorithm.filterchain.tagger.ItineraryTagger;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class is responsible for filtering itineraries. It is instantiated in the
 * {@link ItineraryListFilterChainBuilder}. An instance of {@link ItineraryTagger} should be
 * provided as the constructor parameter.
 */
public class FilteringFilter implements ItineraryListFilter {
    private final ItineraryTagger tagger;

    public FilteringFilter(ItineraryTagger tagger) {
        this.tagger = tagger;
    }

    public String name() {
        return tagger.name();
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        List<Itinerary> filterInput;
        if (tagger.processUntaggedItinerariesOnly()) {
            filterInput = itineraries.stream()
                    .filter(Predicate.not(Itinerary::isMarkedAsDeleted))
                    .collect(Collectors.toList());
        } else {
            filterInput = itineraries;
        }

        tagger.tagItineraries(filterInput);
        return itineraries;
    }
}
