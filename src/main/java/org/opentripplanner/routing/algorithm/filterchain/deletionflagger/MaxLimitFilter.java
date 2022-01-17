package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import com.google.common.collect.Lists;
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
    private final boolean reverseFilteringDirection;
    private final Consumer<Itinerary> changedSubscriber;

    public MaxLimitFilter(String name, int maxLimit) {
        this(name, maxLimit, false, null);
    }

    public MaxLimitFilter(
            String name,
            int maxLimit,
            boolean reverseFilteringDirection,
            Consumer<Itinerary> changedSubscriber
    ) {
        this.name = name;
        this.maxLimit = maxLimit;
        this.reverseFilteringDirection = reverseFilteringDirection;
        this.changedSubscriber = changedSubscriber == null ? IGNORE_SUBSCRIBER : changedSubscriber;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<Itinerary> getFlaggedItineraries(List<Itinerary> itineraries) {
        if(itineraries.size() <= maxLimit) { return List.of(); }
        List<Itinerary> possiblyReversedItineraries = reverseFilteringDirection
                ? Lists.reverse(itineraries)
                : itineraries;
        changedSubscriber.accept(possiblyReversedItineraries.get(maxLimit));
        return possiblyReversedItineraries.stream().skip(maxLimit).collect(Collectors.toList());
    }
}
