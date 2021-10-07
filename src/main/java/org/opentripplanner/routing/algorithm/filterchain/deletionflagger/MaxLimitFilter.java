package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;

import java.util.List;
import java.util.function.Consumer;


/**
 * Flag all itineraries after the provided limit. This flags the itineraries at the end of the list
 * for removal, so the list should be sorted on the desired key before this filter is applied.
 * <p>
 * The filter can also report the first itinerary in the list it will flag. The subscriber
 * is optional.
 */
public class MaxLimitFilter implements ItineraryDeletionFlagger {
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
    public List<Itinerary> getFlaggedItineraries(List<Itinerary> itineraries) {
        if(itineraries.size() <= maxLimit) { return List.of(); }
        changedSubscriber.accept(itineraries.get(maxLimit));
        return itineraries.stream().skip(maxLimit).collect(Collectors.toList());
    }
}
