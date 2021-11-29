package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;

class OtherThanSameLegsMaxGeneralizedCostFilterTest implements PlanTestConstants {

    @Test
    public void testFilter() {

        Itinerary first = newItinerary(A)
                .rail(20, T11_05, T11_14, B)
                .bus(30, T11_16, T11_20, C)
                .build();

        Itinerary second = newItinerary(A)
                .rail(20, T11_05, T11_14, B)
                .walk(D10m, C)
                .build();

        var subject = new OtherThanSameLegsMaxGeneralizedCostFilter(2.0);
        assertEquals(List.of(second), subject.getFlaggedItineraries(List.of(first, second)));
    }

}