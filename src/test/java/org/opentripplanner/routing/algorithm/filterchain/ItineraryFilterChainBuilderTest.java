package org.opentripplanner.routing.algorithm.filterchain;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.TestItineraryBuilder;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.A;
import static org.opentripplanner.model.plan.TestItineraryBuilder.E;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

public class ItineraryFilterChainBuilderTest {
    // Given a default chain
    private final ItineraryFilterChainBuilder builder = new ItineraryFilterChainBuilder(false);

    // And some itineraries, with some none optimal options
    private final Itinerary i1 = newItinerary(A, 6).walk(2, E).build();

    // Not optimal, takes longer than walking
    private final Itinerary i2 = newItinerary(A).bus(21,6, 9, E).build();

    // Not optimal, departure is very late
    private final Itinerary i3 = newItinerary(A).bus(20,50, 51, E).build();


    @Test
    public void testDefaultFilterChain() {
        // Given a default chain
        ItineraryFilter chain = builder.build();

        assertEquals(toStr(List.of(i1, i3)), toStr(chain.filter(List.of(i1, i2, i3))));
    }

    @Test
    public void testFilterChainWithLateDepartureFilterSet() {
        // Given a default chain
        builder.setLatestDepartureTimeLimit(TestItineraryBuilder.newTime(40).toInstant());
        ItineraryFilter chain = builder.build();

        assertEquals(toStr(List.of(i1)), toStr(chain.filter(List.of(i1, i3))));
    }

    @Test
    public void testDebugFilterChain() {
        // Given a filter-chain with debugging enabled
        builder.debug();
        builder.removeTransitWithHigherCostThanBestOnStreetOnly(true);
        builder.setLatestDepartureTimeLimit(TestItineraryBuilder.newTime(40).toInstant());
        builder.setApproximateMinLimit(1);
        builder.setMaxLimit(1);

        ItineraryFilter chain = builder.build();

        // Walk first, then transit sorted on arrival-time
        assertEquals(toStr(List.of(i1, i2, i3)), toStr(chain.filter(List.of(i1, i2, i3))));
        assertTrue(i1.systemNotices.isEmpty());
        assertFalse(i2.systemNotices.isEmpty());
        assertFalse(i3.systemNotices.isEmpty());
        assertEquals("transit-vs-street-filter", i2.systemNotices.get(0).tag);
        assertEquals("latest-departure-time-limit", i3.systemNotices.get(0).tag);
    }

    @Test
    public void testFilterChainWithMaxItinerariesFilterSet() {
        // Given a default chain
        builder.setMaxLimit(1);
        builder.setApproximateMinLimit(1);
        ItineraryFilter chain = builder.build();

        assertEquals(List.of(i1), chain.filter(List.of(i1, i2, i3)));
    }
}