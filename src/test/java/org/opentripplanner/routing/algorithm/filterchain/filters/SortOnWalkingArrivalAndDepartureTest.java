package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.A;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.B;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.C;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.D;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.E;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.itinerary;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.leg;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.toStr;

public class SortOnWalkingArrivalAndDepartureTest {
    private final Itinerary I1 = itinerary(leg(A, C, 6, 8, 1.0, TraverseMode.WALK));
    private final Itinerary I2 = itinerary(leg(A, D, 7, 9, 1.0, TraverseMode.WALK));
    private final Itinerary I3 = itinerary(leg(A, E, 4, 9, 1.0, TraverseMode.WALK));
    private final Itinerary I4 = itinerary(leg(B, C, 6, 8, 1.0, TraverseMode.TRANSIT));
    private final Itinerary I5 = itinerary(leg(B, D, 7, 9, 1.0, TraverseMode.TRANSIT));
    private final Itinerary I6 = itinerary(leg(B, E, 4, 9, 1.0, TraverseMode.TRANSIT));

    private SortOnWalkingArrivalAndDeparture subject = new SortOnWalkingArrivalAndDeparture();

    @Test
    public void sortSetOfWalkingItineraries() {
        List<Itinerary> result;

        result = subject.filter(List.of(I2, I3, I1));
        assertEquals(toStr(result), List.of(I1, I2, I3), result);

        result = subject.filter(List.of(I3, I1, I2));
        assertEquals(toStr(result), List.of(I1, I2, I3), result);
    }

    @Test
    public void sortSetOfTransitItineraries() {
        List<Itinerary> result = subject.filter(List.of(I4, I6, I5));

        // The expected sort order is the WALK leg first, then
        assertEquals(toStr(result), List.of(I4, I5, I6), result);
    }

    @Test
    public void verifyWalkingArrivalTimeAndDepartureTime() {
        // Then sort, insert in random order
        List<Itinerary> result = subject.filter(List.of(I6, I3, I2, I4, I5, I1));

        // The expected sort order is the WALK leg first, then
        assertEquals(toStr(result), toStr(List.of(I1, I2, I3, I4, I5, I6)), toStr(result));
    }

}