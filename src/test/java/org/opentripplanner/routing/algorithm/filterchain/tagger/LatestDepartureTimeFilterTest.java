package org.opentripplanner.routing.algorithm.filterchain.tagger;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

import java.time.Instant;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

public class LatestDepartureTimeFilterTest implements PlanTestConstants {

    @Test
    public void filterOnLatestDepartureTime() {
        // Given:
        Itinerary it = newItinerary(A).bus(32, 0, 60, E).build();
        Instant time = it.firstLeg().startTime.toInstant();

        // When:
        assertTrue(process(List.of(it), new LatestDepartureTimeFilter(time.minusSeconds(1))).isEmpty());

        // Remove notice after asserting
        it.systemNotices.remove(0);

        assertFalse(process(List.of(it), new LatestDepartureTimeFilter(time)).isEmpty());
    }

    private List<Itinerary> process(List<Itinerary> itineraries, LatestDepartureTimeFilter filter) {
        filter.tagItineraries(itineraries);
        return itineraries.stream()
                .filter(Predicate.not(Itinerary::isMarkedAsDeleted))
                .collect(Collectors.toList());
    }
}