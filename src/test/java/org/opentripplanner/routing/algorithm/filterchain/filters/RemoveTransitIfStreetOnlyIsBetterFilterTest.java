package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

public class RemoveTransitIfStreetOnlyIsBetterFilterTest implements PlanTestConstants {

    @Test
    public void filterAwayNothingIfNoWalking() {
        // Given:
        Itinerary i1 = newItinerary(A).bus(21, 6, 7, E).build();
        Itinerary i2 = newItinerary(A).rail(110, 6, 9, E).build();

        // When:
        List<Itinerary> result = new RemoveTransitIfStreetOnlyIsBetterFilter().filter(List.of(i1, i2));

        // Then:
        assertEquals(toStr(List.of(i1, i2)), toStr(result));
    }

    @Test
    public void filterAwayLongTravelTimeWithoutWaitTime() {
        // Given: a walk itinerary with high cost - do not have any effect on filtering
        Itinerary walk = newItinerary(A, 6).walk(1, E).build();
        walk.generalizedCost = 300;

        // Given: a bicycle itinerary with low cost - transit with higher cost is removed
        Itinerary bicycle = newItinerary(A).bicycle(6, 8, E).build();
        bicycle.generalizedCost = 200;

        Itinerary i1 = newItinerary(A).bus(21, 6, 8, E).build();
        i1.generalizedCost = 199;

        Itinerary i2 = newItinerary(A).bus(31, 6, 8, E).build();
        i2.generalizedCost = 200;

        // When:
        List<Itinerary> result = new RemoveTransitIfStreetOnlyIsBetterFilter()
            .filter(List.of(i2, bicycle, walk, i1));

        // Then:
        assertEquals(toStr(List.of(bicycle, walk, i1)), toStr(result));
    }
}