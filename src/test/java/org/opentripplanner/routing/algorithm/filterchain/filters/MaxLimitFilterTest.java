package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.A;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.E;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.itinerary;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.leg;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.toStr;

public class MaxLimitFilterTest {
    private final Itinerary i1 = itinerary(leg(A, E, 6, 7, 5.0, TraverseMode.WALK));
    private final Itinerary i2 = itinerary(leg(A, E, 6, 8, 4.0, TraverseMode.BICYCLE));
    private final Itinerary i3 = itinerary(leg(A, E, 6, 8, 4.0, TraverseMode.BUS));

    private final List<Itinerary> itineraries = List.of(i1, i2, i3);


    @Test
    public void name() {
        MaxLimitFilter subject = new MaxLimitFilter("Test", 3);
        assertEquals("Test", subject.name());
    }

    @Test
    public void testNormalFilter() {
        MaxLimitFilter subject;

        subject = new MaxLimitFilter("Test", 3);
        assertEquals(toStr(itineraries), toStr(subject.filter(itineraries)));

        subject = new MaxLimitFilter("Test", 1);
        assertEquals(toStr(List.of(i1)), toStr(subject.filter(itineraries)));

        subject = new MaxLimitFilter("Test", 0);
        assertEquals(toStr(Collections.emptyList()), toStr(subject.filter(itineraries)));
    }
}