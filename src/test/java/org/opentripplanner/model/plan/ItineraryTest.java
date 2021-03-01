package org.opentripplanner.model.plan;

import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newTime;

public class ItineraryTest implements PlanTestConstants {
    @Test
    public void testDerivedFieldsWithWalkingOnly() {
        Itinerary result = newItinerary(A, T11_00).walk(D5m, B).build();

        // Expected fields on itinerary set
        assertEquals(300, result.durationSeconds);
        assertEquals(0, result.nTransfers);
        assertEquals(600, result.generalizedCost);
        assertEquals(0, result.transitTimeSeconds);
        assertEquals(300, result.nonTransitTimeSeconds);
        assertEquals(0, result.waitingTimeSeconds);
        assertTrue(result.walkOnly);

        // Expected fields on walking leg set
        assertSameLocation(A, result.firstLeg().from);
        assertEquals(GregorianCalendar.from(newTime(T11_00)), result.firstLeg().startTime);
        assertEquals(GregorianCalendar.from(newTime(T11_05)), result.firstLeg().endTime);
        assertEquals(TraverseMode.WALK, result.firstLeg().mode);
        assertEquals(420.0d, result.firstLeg().distanceMeters, 1E-3);
        assertSameLocation(B, result.lastLeg().to);

        assertEquals("A ~ Walk 5m ~ B [cost: 600]", result.toStr());
    }

    @Test
    public void testDerivedFieldsWithBusAllTheWay() {
        Itinerary result = newItinerary(A).bus(55, T11_00, T11_10, B).build();

        assertEquals(600, result.durationSeconds);
        assertEquals(0, result.nTransfers);
        assertEquals(720, result.generalizedCost);
        assertEquals(600, result.transitTimeSeconds);
        assertEquals(0, result.nonTransitTimeSeconds);
        assertEquals(0, result.waitingTimeSeconds);
        assertFalse(result.walkOnly);

        // Expected fields on bus leg set
        assertSameLocation(A, result.firstLeg().from);
        assertSameLocation(B, result.firstLeg().to);
        assertEquals(GregorianCalendar.from(newTime(T11_00)), result.firstLeg().startTime);
        assertEquals(GregorianCalendar.from(newTime(T11_10)), result.firstLeg().endTime);
        assertEquals(TraverseMode.BUS, result.firstLeg().mode);
      assertEquals(new FeedScopedId("F", "55"), result.firstLeg().getTrip().getId());
        assertEquals(7500, result.firstLeg().distanceMeters, 1E-3);

        assertEquals("A ~ BUS 55 11:00 11:10 ~ B [cost: 720]", result.toStr());
    }

    @Test
    public void testDerivedFieldsWithTrainAllTheWay() {
        Itinerary result = newItinerary(A).rail(20, T11_05, T11_15, B).build();

        assertEquals(600, result.durationSeconds);
        assertEquals(0, result.nTransfers);
        assertEquals(720, result.generalizedCost);
        assertEquals(600, result.transitTimeSeconds);
        assertEquals(0, result.nonTransitTimeSeconds);
        assertEquals(0, result.waitingTimeSeconds);
        assertFalse(result.walkOnly);

        // Expected fields on bus leg set
        assertSameLocation(A, result.firstLeg().from);
        assertSameLocation(B, result.firstLeg().to);
        assertEquals(GregorianCalendar.from(newTime(T11_05)), result.firstLeg().startTime);
        assertEquals(GregorianCalendar.from(newTime(T11_15)), result.firstLeg().endTime);
        assertEquals(TraverseMode.RAIL, result.firstLeg().mode);
        assertEquals(new FeedScopedId("F", "20"), result.firstLeg().getTrip().getId());
        assertEquals(15_000, result.firstLeg().distanceMeters, 1E-3);

        assertEquals("A ~ RAIL 20 11:05 11:15 ~ B [cost: 720]", result.toStr());
    }

    @Test
    public void testDerivedFieldsWithWalAccessAndTwoTransitLegs() {
        Itinerary itinerary = TestItineraryBuilder.newItinerary(A, T11_02)
            .walk(D1m, B)
            .bus(21, T11_05, T11_10, C)
            .rail(110, T11_15, T11_30, D)
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
        Itinerary result = newItinerary(A, T11_05)
            .walk(D2m, B)
            // 3 minutes wait
            .bus(1, T11_10, T11_20, C)
            .walk(D3m, D)
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
            "A ~ Walk 2m ~ B ~ BUS 1 11:10 11:20 ~ C ~ Walk 3m ~ D [cost: 1464]",
            result.toStr()
        );
    }

    @Test
    public void walkBusBusWalkTrainWalk() {
        Itinerary result = newItinerary(A, T11_00)
            .walk(D2m, B)
            .bus(55, T11_04, T11_14, C)
            .bus(21, T11_16, T11_20, D)
            .walk(D3m, E)
            .rail(20, T11_30, T11_50, F)
            .walk(D1m, G)
            .build();

        assertEquals(3060, result.durationSeconds);
        assertEquals(2, result.nTransfers);
        assertEquals(2040, result.transitTimeSeconds);
        assertEquals(360, result.nonTransitTimeSeconds);
        assertEquals(660, result.waitingTimeSeconds);
        assertEquals(720 + 528 + 360 + 2040, result.generalizedCost);
        assertFalse(result.walkOnly);
        assertSameLocation(A, result.firstLeg().from);
        assertSameLocation(G, result.lastLeg().to);

        assertEquals(
            "A ~ Walk 2m ~ B ~ BUS 55 11:04 11:14 ~ C ~ BUS 21 11:16 11:20 ~ D "
                + "~ Walk 3m ~ E ~ RAIL 20 11:30 11:50 ~ F ~ Walk 1m ~ G [cost: 3648]",
            result.toStr()
        );
    }

    private void assertSameLocation(Place expected, Place actual) {
        assertTrue(
            "Same location? Expected: " + expected + ", actual: " + actual,
            expected.sameLocation(actual));
    }
}