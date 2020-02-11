package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.A;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.B;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.E;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.itinerary;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.leg;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.toStr;

public class LongTransitWalkingFilterTest {

    @Test
    public void filterAwayNothingIfNoWalking() {
        // Given:
        Itinerary i1 = itinerary(leg(A, E, 6, 7, 5.0, TraverseMode.BICYCLE));
        Itinerary i2 = itinerary(leg(A, E, 6, 9, 5.0, TraverseMode.TRANSIT));

        // When:
        List<Itinerary> result = new LongTransitWalkingFilter().filter(List.of(i1, i2));

        // Then:
        assertEquals(toStr(result), List.of(i1, i2), result);
    }

    @Test
    public void filterAwayLongTravelTimeWithoutWaitTime() {
        // Given: a walk leg, 2 hours
        Itinerary walk = itinerary(leg(A, E, 6, 7, 5.0, TraverseMode.WALK));

        Itinerary i1 = itinerary(leg(A, E, 6, 8, 4.0, TraverseMode.CAR));
        Itinerary i2 = itinerary(leg(A, E, 6, 8, 4.0, TraverseMode.BICYCLE));
        Itinerary i3 = itinerary(leg(A, E, 6, 8, 4.0, TraverseMode.BUS));

        // When:
        List<Itinerary> result = new LongTransitWalkingFilter().filter(List.of(i1, i2, i3, walk));

        // Then:
        assertEquals(toStr(result), List.of(walk), result);
    }

    @Test
    public void doNotFilterAwayItinerariesWithLongWaitTimes() {
        // Given: a walk leg, 3 hours
        Itinerary walk = itinerary(leg(A, E, 6, 9, 5.0, TraverseMode.WALK));


        // Travel time exceeds walking: 3 hours
        Itinerary i1 = itinerary(
                leg(A, B, 6, 7, 5.0, TraverseMode.WALK),
                leg(B, E, 9, 10, 1.0, TraverseMode.TRANSIT)
        );

        // When:
        List<Itinerary> result = new LongTransitWalkingFilter().filter(List.of(walk, i1));

        // Then:
        assertEquals(toStr(result), List.of(walk, i1), result);
    }

}