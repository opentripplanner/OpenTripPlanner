package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

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
        Instant time = it.firstLeg().getStartTime().toInstant();

        // When:
        assertTrue(DeletionFlaggerTestHelper.process(List.of(it), new LatestDepartureTimeFilter(time.minusSeconds(1))).isEmpty());
        assertFalse(DeletionFlaggerTestHelper.process(List.of(it), new LatestDepartureTimeFilter(time)).isEmpty());
    }
}