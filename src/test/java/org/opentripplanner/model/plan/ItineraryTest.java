package org.opentripplanner.model.plan;

import org.junit.Test;
import org.opentripplanner.routing.core.TraverseMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.model.plan.ItineraryTestData.A;
import static org.opentripplanner.model.plan.ItineraryTestData.B;
import static org.opentripplanner.model.plan.ItineraryTestData.E;
import static org.opentripplanner.model.plan.ItineraryTestData.itinerary;
import static org.opentripplanner.model.plan.ItineraryTestData.leg;

public class ItineraryTest {

    @Test
    public void testDerivedFieldsWithWalkingAndTransit() {
        Itinerary itinerary = itinerary(
                leg(A, B, 6, 7, 5.0, TraverseMode.WALK),
                leg(B, E, 8, 10, 3.0, TraverseMode.TRANSIT),
                leg(B, E, 12, 13, 1.0, TraverseMode.TRANSIT)
        );

        assertEquals(1, itinerary.nTransfers);

        assertEquals(7 * 60, itinerary.durationSeconds);
        assertEquals(3 * 60, itinerary.transitTimeSeconds);
        assertEquals(1 * 60, itinerary.nonTransitTimeSeconds);
        assertEquals(3 * 60, itinerary.waitingTimeSeconds);

        assertEquals(5.0, itinerary.nonTransitDistanceMeters, 0.01);
        assertFalse(itinerary.walkOnly);
    }

    @Test
    public void testDerivedFieldsWithWalkingOnly() {
        // Just walking
        Itinerary itinerary = itinerary(leg(A, B, 6, 8, 3.5, TraverseMode.WALK));

        assertEquals(0, itinerary.nTransfers);

        assertEquals(2 * 60, itinerary.durationSeconds);
        assertEquals(0, itinerary.transitTimeSeconds);
        assertEquals(2 * 60, itinerary.nonTransitTimeSeconds);
        assertEquals(0, itinerary.waitingTimeSeconds);

        assertEquals(3.5, itinerary.nonTransitDistanceMeters, 0.01);
        assertTrue(itinerary.walkOnly);
    }
}