package org.opentripplanner.routing.algorithm.filterchain.groupids;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.PlanTestConstants;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.routing.algorithm.filterchain.groupids.GroupByTripIdAndDistance.calculateTotalDistance;
import static org.opentripplanner.routing.algorithm.filterchain.groupids.GroupByTripIdAndDistance.getKeySetOfLegsByLimit;

public class GroupByTripIdAndDistanceTest implements PlanTestConstants {

    @Test
    public void calculateTotalDistanceTest() {
        Itinerary i = newItinerary(A)
            .bus(21, T11_01, T11_02, B)
            .walk(D2m, C)
            .bus(31, T11_05, T11_07, D)
            .build();

        Leg l1 = i.legs.get(0);
        Leg l2 = i.legs.get(1);
        Leg l3 = i.legs.get(2);

        // 3 minutes on a bus
        double expectedDistanceRidingABus = BUS_SPEED * 3 * 60;
        // 2 minute walking
        double expectedDistanceWalking = WALK_SPEED * 2 * 60;
        // total
        double expectedDistance = expectedDistanceRidingABus + expectedDistanceWalking;

        assertEquals(expectedDistanceRidingABus, calculateTotalDistance(List.of(l1, l3)),0.001);
        assertEquals(expectedDistanceWalking, calculateTotalDistance(List.of(l2)),0.001);
        assertEquals(expectedDistance, calculateTotalDistance(List.of(l1, l2, l3)),0.001);
    }

    @Test
    public void getKeySetOfLegsByLimitTest() {
        Itinerary i = newItinerary(A)
            // 5 min bus ride
            .bus(11, T11_00, T11_05, B)
            // 2 min buss ride
            .bus(21, T11_10, T11_12, C)
            // 3 min buss ride
            .bus(31, T11_20, T11_23, D)
            .build();

        Leg l1 = i.legs.get(0);
        Leg l2 = i.legs.get(1);
        Leg l3 = i.legs.get(2);

        Double d1 = l1.distanceMeters;
        Double d3 = l3.distanceMeters;

        // These test relay on the internal sort by distance, witch make the implementation
        // a bit simpler, but strictly is not something the method grantees
        assertEquals(
                List.of(l1),
                getKeySetOfLegsByLimit(List.of(l1, l2, l3), d1 - 0.01)
        );
        assertEquals(
                List.of(l1, l3),
                getKeySetOfLegsByLimit(List.of(l1, l2, l3), d1 + 0.01)
        );
        assertEquals(
                List.of(l1, l3),
                getKeySetOfLegsByLimit(List.of(l1, l2, l3), d1 + d3 - 0.01)
        );
        assertEquals(
                List.of(l1, l3, l2),
                getKeySetOfLegsByLimit(List.of(l1, l2, l3), d1 + d3 + 0.01)
        );
    }

    @Test
    public void nonTransitShouldHaveAEmptyKey() {
        GroupByTripIdAndDistance shortTransit = new GroupByTripIdAndDistance(
            newItinerary(A, T11_00).walk(D10m, A).build(),
            0.5
        );
        assertEquals(0, shortTransit.getKeySet().size());
    }

    @Test
    public void shortTransitShouldBeTreatedAsNonTransit() {
        GroupByTripIdAndDistance shortTransit = new GroupByTripIdAndDistance(
            // walk 30 minutes, bus 1 minute => walking account for more than 50% of the distance
            newItinerary(A, T11_00).walk(D10m, A).bus(11, T11_32, T11_33, B).build(),
            0.5
        );
        // Make sure transit have 1 leg in the key-set
        assertEquals(0, shortTransit.getKeySet().size());
    }

    @Test
    public void mergeBasedOnKeySetSize() {
        GroupByTripIdAndDistance oneLeg = new GroupByTripIdAndDistance(
            newItinerary(A).bus(11, T11_00, T11_30, D).build(),0.8
        );
        GroupByTripIdAndDistance twoLegs = new GroupByTripIdAndDistance(
            newItinerary(A)
                .bus(11, T11_00, T11_10, B)
                .bus(21, T11_20, T11_30, D)
                .build(),
            0.8
        );

        // Make sure both legs is part of the key-set
        assertEquals(2, twoLegs.getKeySet().size());

        // Expect merge to return oneLeg every time
        assertSame(oneLeg, oneLeg.merge(twoLegs));
        assertSame(oneLeg, twoLegs.merge(oneLeg));
        assertSame(oneLeg, oneLeg.merge(oneLeg));
    }

    @Test
    public void transitDoesNotMatchEmptyKeySet() {
        GroupByTripIdAndDistance transit = new GroupByTripIdAndDistance(
            newItinerary(A).bus(11, T11_00, T11_05, B).build(),0.5
        );
        GroupByTripIdAndDistance nonTransit = new GroupByTripIdAndDistance(
            newItinerary(A, T11_00).walk(D5m, B).build(),0.5
        );
        // Make sure transit have 1 leg in the key-set
        assertEquals(1, transit.getKeySet().size());

        assertFalse(transit.match(nonTransit));
        assertFalse(nonTransit.match(transit));
    }

    @Test
    public void twoNonTransitKeySetShouldNotMatch() {
        GroupByTripIdAndDistance nonTransitA = new GroupByTripIdAndDistance(
            newItinerary(A, T11_00).walk(D5m, B).build(),0.5
        );
        GroupByTripIdAndDistance nonTransitB = new GroupByTripIdAndDistance(
            newItinerary(A, T11_00).walk(D5m, B).build(),0.5
        );
        assertFalse(nonTransitA.match(nonTransitB));
        assertFalse(nonTransitB.match(nonTransitA));
        assertTrue(nonTransitA.match(nonTransitA));
    }

    @Test
    public void matchDifferentTransitKeySet() {
        GroupByTripIdAndDistance g_11 = new GroupByTripIdAndDistance(
            newItinerary(A).bus(11, T11_00, T11_05, E).build(),0.9
        );
        GroupByTripIdAndDistance g_21 = new GroupByTripIdAndDistance(
            newItinerary(A).bus(21, T11_00, T11_05, E).build(),0.9
        );
        GroupByTripIdAndDistance g_11_21 = new GroupByTripIdAndDistance(
            newItinerary(A)
                .bus(11, T11_00, T11_03, D)
                .bus(21, T11_04, T11_06, E)
                .build(),
            0.9
        );
        GroupByTripIdAndDistance g_31_11 = new GroupByTripIdAndDistance(
            newItinerary(A)
                .bus(31, T11_01, T11_03, B)
                .bus(11, T11_04, T11_06, E)
                .build(),
            0.9
        );

        // Match itself
        assertTrue(g_11.match(g_11));
        // Match other with suffix leg
        assertTrue(g_11.match(g_11_21));
        assertTrue(g_11_21.match(g_11));
        // Match other with prefix leg
        assertTrue(g_11.match(g_31_11));
        assertTrue(g_31_11.match(g_11));

        // Do not match
        assertFalse(g_11.match(g_21));
        assertFalse(g_21.match(g_11));
        assertFalse(g_11_21.match(g_31_11));
        assertFalse(g_31_11.match(g_11_21));
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalRangeForPUpperBound() {
        new GroupByTripIdAndDistance(
            newItinerary(A).bus(21, T11_01, T11_02, E).build(),
            0.991
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalRangeForPLowerBound() {
        new GroupByTripIdAndDistance(
            newItinerary(A).bus(21, T11_01, T11_02, E).build(),
            0.499
        );
    }
}