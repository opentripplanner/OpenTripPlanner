package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.A;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.E;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.itinerary;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.leg;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.toStr;

public class RemoveTransitIfStreetOnlyIsBetterFilterTest {

    @Test
    public void filterAwayNothingIfNoWalking() {
        // Given:
        Itinerary i1 = itinerary(leg(A, E, 6, 7, 5.0, TraverseMode.BUS));
        Itinerary i2 = itinerary(leg(A, E, 6, 9, 5.0, TraverseMode.RAIL));

        // When:
        List<Itinerary> result = new RemoveTransitIfStreetOnlyIsBetterFilter().filter(List.of(i1, i2));

        // Then:
        assertEquals(toStr(result), List.of(i1, i2), result);
    }

    @Test
    public void filterAwayLongTravelTimeWithoutWaitTime() {
        // Given: a walk itinerary with high cost - do not have any effect on filtering
        Itinerary walk = itinerary(leg(A, E, 6, 7, 5.0, TraverseMode.WALK));
        walk.generalizedCost = 300;

        // Given: a bicycle itinerary with low cost - transit with higher cost is removed
        Itinerary bicycle = itinerary(leg(A, E, 6, 8, 4.0, TraverseMode.BICYCLE));
        bicycle.generalizedCost = 200;

        Itinerary i1 = itinerary(leg(A, E, 6, 8, 4.0, TraverseMode.BUS));
        i1.generalizedCost = 199;

        Itinerary i2 = itinerary(leg(A, E, 6, 8, 4.0, TraverseMode.BUS));
        i2.generalizedCost = 200;

        // When:
        List<Itinerary> result = new RemoveTransitIfStreetOnlyIsBetterFilter()
            .filter(List.of(i2, bicycle, walk, i1));

        // Then:
        assertEquals(toStr(List.of(bicycle, walk, i1)), toStr(result));
    }
}