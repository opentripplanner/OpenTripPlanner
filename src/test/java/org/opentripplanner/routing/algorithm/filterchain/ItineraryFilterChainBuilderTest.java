package org.opentripplanner.routing.algorithm.filterchain;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.A;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.E;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.itinerary;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.leg;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.newTime;
import static org.opentripplanner.routing.core.TraverseMode.TRANSIT;
import static org.opentripplanner.routing.core.TraverseMode.WALK;

public class ItineraryFilterChainBuilderTest {
    // Given a default chain
    private ItineraryFilterChainBuilder builder = new ItineraryFilterChainBuilder();

    // And some itineraries, with some none optimal option
    private Itinerary i1 = itinerary(leg(A, E, 6, 7, 5.0, WALK));

    // Not optimal, takes longer than walking
    private Itinerary i2 = itinerary(leg(A, E, 6, 9, 5.0, TRANSIT));

    // Not optimal, departure is very late
    private Itinerary i3 = itinerary(leg(A, E, 50, 51, 5.0, TRANSIT));


    @Test
    public void testDefaultFilterChain() {
        // Given a default chain
        ItineraryFilter chain = builder.build();

        assertEquals(List.of(i1, i3), chain.filter(List.of(i1, i2, i3)));
    }

    @Test
    public void testFilterChainWithLateDepartureFilterSet() {
        // Given a default chain
        builder.setLatestDepartureTimeLimit(newTime(40).toInstant());
        ItineraryFilter chain = builder.build();

        assertEquals(List.of(i1), chain.filter(List.of(i1, i3)));
    }

    @Test
    public void testDebugFilterChain() {
        // Given a filter-chain with debugging enabled
        builder.debug();

        ItineraryFilter chain = builder.build();

        assertEquals(List.of(i1, i2), chain.filter(List.of(i1, i2)));
        assertFalse(i1.hasSystemNotices());
        assertTrue(i2.hasSystemNotices());
        assertEquals("transit-walking-filter", i2.systemNotices.get(0).tag);
    }
}