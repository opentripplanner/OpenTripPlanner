package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

import java.time.Instant;
import java.util.List;

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
        assertTrue(new LatestDepartureTimeFilter(time.minusSeconds(1)).filter(List.of(it)).isEmpty());
        assertFalse(new LatestDepartureTimeFilter(time).filter(List.of(it)).isEmpty());
    }
}