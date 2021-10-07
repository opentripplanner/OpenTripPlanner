package org.opentripplanner.routing.algorithm.filterchain.tagger;

import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;

import java.util.List;
import java.util.function.Consumer;


/**
 * Remove all itineraries after the provided limit. This filter remove the itineraries at the
 * end of the list, so the list should be sorted on the desired key before this filter is applied.
 * <p>
 * The filter can also report the first itinerary in the list it will remove. The subscriber
 * is optional.
 */
public class MaxLimitFilter implements ItineraryTagger {
    private static final Consumer<Itinerary> IGNORE_SUBSCRIBER = (i) -> {};

    private final String name;
    private final int maxLimit;
    private final Consumer<Itinerary> changedSubscriber;

    public MaxLimitFilter(String name, int maxLimit) {
        this(name, maxLimit, null);
    }

    public MaxLimitFilter(String name, int maxLimit, Consumer<Itinerary> changedSubscriber) {
        this.name = name;
        this.maxLimit = maxLimit;
        this.changedSubscriber = changedSubscriber == null ? IGNORE_SUBSCRIBER : changedSubscriber;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<Itinerary> getTaggedItineraries(List<Itinerary> itineraries) {
        if(itineraries.size() <= maxLimit) { return List.of(); }
        changedSubscriber.accept(itineraries.get(maxLimit));
        return itineraries.stream().skip(maxLimit).collect(Collectors.toList());
    }
}
