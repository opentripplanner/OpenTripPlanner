package org.opentripplanner.model.plan;

import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.core.TraverseMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newTime;

public class ItineraryTest {
    @Test
    public void testDerivedFieldsWithWalkingOnly() {
        Itinerary result = newItinerary(TestItineraryBuilder.A, 7).walk(5, TestItineraryBuilder.B).build();

        // Expected fields on itinerary set
        assertEquals(300, result.durationSeconds);
        assertEquals(0, result.nTransfers);
        assertEquals(600, result.generalizedCost);
        assertEquals(0, result.transitTimeSeconds);
        assertEquals(300, result.nonTransitTimeSeconds);
        assertEquals(0, result.waitingTimeSeconds);
        assertTrue(result.walkOnly);

        // Expected fields on walking leg set
        assertSameLocation(TestItineraryBuilder.A, result.firstLeg().from);
        assertEquals(newTime(7), result.firstLeg().startTime);
        assertEquals(newTime(12), result.firstLeg().endTime);
        assertEquals(TraverseMode.WALK, result.firstLeg().mode);
        assertEquals(420.0d, result.firstLeg().distanceMeters, 1E-3);
        assertSameLocation(TestItineraryBuilder.B, result.lastLeg().to);

        assertEquals("A ~ Walk 5m ~ B [cost: 600]", result.toStr());
    }

    @Test
    public void testDerivedFieldsWithBusAllTheWay() {
        Itinerary result = newItinerary(TestItineraryBuilder.A).bus(55, 10, 20,
            TestItineraryBuilder.B
        ).build();

        assertEquals(600, result.durationSeconds);
        assertEquals(0, result.nTransfers);
        assertEquals(720, result.generalizedCost);
        assertEquals(600, result.transitTimeSeconds);
        assertEquals(0, result.nonTransitTimeSeconds);
        assertEquals(0, result.waitingTimeSeconds);
        assertFalse(result.walkOnly);

        // Expected fields on bus leg set
        assertSameLocation(TestItineraryBuilder.A, result.firstLeg().from);
        assertSameLocation(TestItineraryBuilder.B, result.firstLeg().to);
        assertEquals(newTime(10), result.firstLeg().startTime);
        assertEquals(newTime(20), result.firstLeg().endTime);
        assertEquals(TraverseMode.BUS, result.firstLeg().mode);
        assertEquals(new FeedScopedId("B", "55"), result.firstLeg().tripId);
        assertEquals(7500, result.firstLeg().distanceMeters, 1E-3);

        assertEquals("A ~ BUS 55 12:10 12:20 ~ B [cost: 720]", result.toStr());
    }

    @Test
    public void testDerivedFieldsWithTrainAllTheWay() {
        Itinerary result = newItinerary(TestItineraryBuilder.A).rail(20, 5, 15,
            TestItineraryBuilder.B
        ).build();

        assertEquals(600, result.durationSeconds);
        assertEquals(0, result.nTransfers);
        assertEquals(720, result.generalizedCost);
        assertEquals(600, result.transitTimeSeconds);
        assertEquals(0, result.nonTransitTimeSeconds);
        assertEquals(0, result.waitingTimeSeconds);
        assertFalse(result.walkOnly);

        // Expected fields on bus leg set
        assertSameLocation(TestItineraryBuilder.A, result.firstLeg().from);
        assertSameLocation(TestItineraryBuilder.B, result.firstLeg().to);
        assertEquals(newTime(5), result.firstLeg().startTime);
        assertEquals(newTime(15), result.firstLeg().endTime);
        assertEquals(TraverseMode.RAIL, result.firstLeg().mode);
        assertEquals(new FeedScopedId("R", "20"), result.firstLeg().tripId);
        assertEquals(15_000, result.firstLeg().distanceMeters, 1E-3);

        assertEquals("A ~ RAIL 20 12:05 12:15 ~ B [cost: 720]", result.toStr());
    }

    @Test
    public void testDerivedFieldsWithWalAccessAndTwoTransitLegs() {
        Itinerary itinerary = TestItineraryBuilder.newItinerary(TestItineraryBuilder.A, 2)
            .walk(1, TestItineraryBuilder.B)
            .bus(21, 5, 10, TestItineraryBuilder.C)
            .rail(110, 15, 30, TestItineraryBuilder.D)
            .build();

        assertEquals(1, itinerary.nTransfers);
        assertEquals(28 * 60, itinerary.durationSeconds);
        assertEquals(20 * 60, itinerary.transitTimeSeconds);
        assertEquals(60, itinerary.nonTransitTimeSeconds);
        assertEquals((2+5) * 60, itinerary.waitingTimeSeconds);
        // Cost: walk + wait + board + transit = 2 * 60 + .8 * 420 + 2 * 120 + 1200
        assertEquals(1896, itinerary.generalizedCost);

        assertEquals(60 * 1.4, itinerary.nonTransitDistanceMeters, 0.01);
        assertFalse(itinerary.walkOnly);
    }

    @Test
    public void testDerivedFieldsWithBusAndWalkingAccessAndEgress() {
        Itinerary result = newItinerary(TestItineraryBuilder.A, 5)
            .walk(2, TestItineraryBuilder.B)
            // 3 minutes wait
            .bus(1, 10, 20, TestItineraryBuilder.C)
            .walk(3, TestItineraryBuilder.D)
            .build();

        assertEquals(1080, result.durationSeconds);
        assertEquals(0, result.nTransfers);
        assertEquals(600, result.transitTimeSeconds);
        assertEquals(300, result.nonTransitTimeSeconds);
        assertEquals(180, result.waitingTimeSeconds);
        // Cost: walk + wait + board + transit = 2 * 300 + .8 * 180 + 120 + 600
        assertEquals(1464, result.generalizedCost);
        assertFalse(result.walkOnly);

        assertEquals(
            "A ~ Walk 2m ~ B ~ BUS 1 12:10 12:20 ~ C ~ Walk 3m ~ D [cost: 1464]",
            result.toStr()
        );
    }

    @Test
    public void walkBusBusWalkTrainWalk() {
        Itinerary result = newItinerary(TestItineraryBuilder.A, 0)
            .walk(2, TestItineraryBuilder.B)
            .bus(55, 4, 14, TestItineraryBuilder.C)
            .bus(21, 16, 20, TestItineraryBuilder.D)
            .walk(3, TestItineraryBuilder.E)
            .rail(20, 30, 50, TestItineraryBuilder.F)
            .walk(1, TestItineraryBuilder.G)
            .build();

        assertEquals(3060, result.durationSeconds);
        assertEquals(2, result.nTransfers);
        assertEquals(2040, result.transitTimeSeconds);
        assertEquals(360, result.nonTransitTimeSeconds);
        assertEquals(660, result.waitingTimeSeconds);
        assertEquals(720 + 528 + 360 + 2040, result.generalizedCost);
        assertFalse(result.walkOnly);
        assertSameLocation(TestItineraryBuilder.A, result.firstLeg().from);
        assertSameLocation(TestItineraryBuilder.G, result.lastLeg().to);

        assertEquals(
            "A ~ Walk 2m ~ B ~ BUS 55 12:04 12:14 ~ C ~ BUS 21 12:16 12:20 ~ D "
                + "~ Walk 3m ~ E ~ RAIL 20 12:30 12:50 ~ F ~ Walk 1m ~ G [cost: 3648]",
            result.toStr()
        );
    }

    private void assertSameLocation(Place expected, Place actual) {
        assertTrue(
            "Same location? Expected: " + expected + ", actual: " + actual,
            expected.sameLocation(actual));
    }
}