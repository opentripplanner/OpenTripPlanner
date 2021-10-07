package org.opentripplanner.routing.algorithm.filterchain.tagger;

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

    /**
     * Mark all itineraries that should not be visible for the user for deletion using {@link
     * Itinerary#markAsDeleted(SystemNotice)}.
     */
    void tagItineraries(List<Itinerary> itineraries);

    /**
     * Should itineraries already marked for deletion by previous filters be removed from the list
     * passed to {@link ItineraryTagger#tagItineraries(List)}. The default value is true, as usually
     * the already removed itineraries are not needed further in the filter chain.
     */
    default boolean processUntaggedItinerariesOnly() {
        return true;
    }

    /**
     * The notice which is shown to the client when debug is enabled. The default method should be
     * overridden when more information from the individual filter should be visible for the user.
     */
    default SystemNotice notice() {
        return new SystemNotice(
                name(),
                "This itinerary is marked as deleted by the " + name() + " filter."
        );
    }
}
