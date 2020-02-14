package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.TraverseMode;

import java.time.Instant;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.A;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.E;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.itinerary;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.leg;

public class LatestDepartureTimeFilterTest {

    @Test
    public void filterAwayNothingIfNoWalking() {
        // Given:
        Itinerary it = itinerary(leg(A, E, 0, 60, 5.0, TraverseMode.TRANSIT));
        Instant time = it.firstLeg().startTime.toInstant();

        // When:
        assertTrue(new LatestDepartureTimeFilter(time.minusSeconds(1)).filter(List.of(it)).isEmpty());
        assertFalse(new LatestDepartureTimeFilter(time).filter(List.of(it)).isEmpty());
    }
}