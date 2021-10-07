package org.opentripplanner.routing.algorithm.filterchain.tagger;

import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Itinerary;

import java.util.List;

/**
 * ItineraryTagger is used to tag itineraries which should not be presented to the user.
 */
public interface ItineraryTagger {

    /**
     * A name used for debugging the post-processor.
     * <p>
     * Use '-' so separate words like: {@code sort-on-duration-filter}
     */
    String name();

    // Override one:

    /**
     * Override this to create a simple filter, which marks all itineraries for deletion where the
     * predicate returns true.
     */
    default Predicate<Itinerary> predicate() { return null; }

    /**
     * Override this if you need to compare itineraries - all at once, for deciding which should get
     * tagged for removal. All itineraries returned from this function will be tagged for deletion
     * using {@link Itinerary#markAsDeleted(SystemNotice)}.
     */
    default List<Itinerary> getTaggedItineraries(List<Itinerary> itineraries) {
        return itineraries.stream().filter(predicate()).collect(Collectors.toList());
    }

    // Tagging options:

    /**
     * Should itineraries already marked for deletion by previous tagger be removed from the list
     * passed to {@link ItineraryTagger#getTaggedItineraries(List)}. The default value is true, as usually
     * the already removed itineraries are not needed further in the filter chain.
     */
    default boolean processUntaggedItinerariesOnly() {
        return true;
    }
}
