package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.itinerary;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.leg;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.toStr;

public class OtpDefaultSortOrderTest {
    private final Itinerary I1 = itinerary(leg("I1", 6, 7, 1.0, TraverseMode.WALK));
    private final Itinerary I2 = itinerary(leg("I2", 7, 9, 1.0, TraverseMode.WALK));
    private final Itinerary I3 = itinerary(leg("I3", 4, 9, 1.0, TraverseMode.WALK));
    private final Itinerary I4 = itinerary(leg("I4", 7, 8, 1.0, TraverseMode.WALK));
    private final Itinerary I5 = itinerary(leg("I5", 6, 8, 1.0, TraverseMode.TRANSIT));
    private final Itinerary I6 = itinerary(leg("I6", 7, 9, 1.0, TraverseMode.TRANSIT));
    private final Itinerary I7 = itinerary(leg("I7", 4, 9, 1.0, TraverseMode.TRANSIT));
    private final Itinerary I8 = itinerary(leg("I8", 7, 8, 1.0, TraverseMode.TRANSIT));

    private OtpDefaultSortOrder subject;

    @Test
    public void sortSetOfWalkingItinerariesUsingDepartAfter() {
        subject = departAfterSort();
        List<Itinerary> result;

        result = subject.filter(List.of(I2, I1, I4, I3));
        assertEquals(toStr(List.of(I1, I4, I2, I3)), toStr(result));

        result = subject.filter(List.of(I4, I2, I3, I1));
        assertEquals(toStr(List.of(I1, I4, I2, I3)), toStr(result));
    }

    @Test
    public void sortSetOfWalkingItinerariesUsingArriveBy() {
        // Arrive by sort order
        subject = arriveBySort();
        List<Itinerary> result;

        result = subject.filter(List.of(I2, I4, I3, I1));
        assertEquals(toStr(List.of(I4, I2, I1, I3)), toStr(result));

        // Repeat test with different initial order
        result = subject.filter(List.of(I3, I1, I2, I4));
        assertEquals(toStr(List.of(I4, I2, I1, I3)), toStr(result));
    }

    @Test
    public void sortSetOfTransitItineraries() {
        List<Itinerary> itinerariesRndOrder = List.of(I5, I7, I6, I8);
        List<Itinerary> result;

        // depart after
        result = departAfterSort().filter(itinerariesRndOrder);
        assertEquals(toStr(List.of(I8, I5, I6, I7)), toStr(result));

        // arrive by
        result = arriveBySort().filter(itinerariesRndOrder);
        assertEquals(toStr(List.of(I8, I6, I5, I7)), toStr(result));

    }

    @Test
    public void verifyWalkingArrivalTimeAndDepartureTime() {
        // Then sort, insert in random order
        List<Itinerary> input = List.of(I7, I3, I8, I4, I2, I5, I6, I1);
        List<Itinerary> result;

        // departAfter
        result = departAfterSort().filter(input);
        // The expected sort order is the WALK leg first, then
        assertEquals(toStr(result), toStr(List.of(I1, I4, I2, I3, I8, I5, I6, I7)), toStr(result));

        // arriveBy
        result = arriveBySort().filter(input);
        // The expected sort order is the WALK leg first, then
        assertEquals(toStr(result), toStr(List.of(I4, I2, I1, I3, I8, I6, I5, I7)), toStr(result));
    }


    private OtpDefaultSortOrder arriveBySort() {
        return new OtpDefaultSortOrder(true);
    }

    private OtpDefaultSortOrder departAfterSort() {
        return new OtpDefaultSortOrder(false);
    }
}