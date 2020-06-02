package org.opentripplanner.routing.algorithm.filterchain.groupids;

import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.TestItineraryBuilder;

import java.util.List;

import static org.opentripplanner.model.plan.TestItineraryBuilder.A;
import static org.opentripplanner.model.plan.TestItineraryBuilder.B;
import static org.opentripplanner.model.plan.TestItineraryBuilder.C;
import static org.opentripplanner.model.plan.TestItineraryBuilder.D;
import static org.opentripplanner.model.plan.TestItineraryBuilder.E;
import static org.opentripplanner.model.plan.TestItineraryBuilder.F;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

public class GroupByLongestLegsIdTest {
    @Test
    public void filterTransitLegs() {
        Leg l1 = newItinerary(A, 1).walk(1, B).build().firstLeg();
        Leg l2 = newItinerary(B).bus(11, 3, 4, C).build().firstLeg();

        List<Leg> legs = GroupByLongestLegsId.filterTransitLegs(List.of(l1, l2));

        Assert.assertEquals(List.of(l2), legs);
    }


    @Test
    public void calculateTotalDistance() {
        Leg l1 = newItinerary(A).bus(21, 1, 2, B).build().firstLeg();
        Leg l2 = newItinerary(B).bus(31, 3, 5, C).build().firstLeg();


        // 3 minutes on a bus
        double expectedDistance = TestItineraryBuilder.BUS_SPEED * 60 * 3;

        Assert.assertEquals(
                expectedDistance,
                GroupByLongestLegsId.calculateTotalDistance(List.of(l1, l2)),
                0.0001
        );
    }

    @Test
    public void findLegsByLimit() {
        Leg l1 = newItinerary(A).bus(21, 1, 6, B).build().firstLeg();
        Leg l2 = newItinerary(B).bus(31, 1, 13, C).build().firstLeg();
        Leg l3 = newItinerary(C).bus(41, 1, 4, D).build().firstLeg();

        Double d1 = l1.distanceMeters;
        Double d2 = l2.distanceMeters;

        Assert.assertEquals(
                List.of(l2),
                GroupByLongestLegsId.findLegsByLimit(List.of(l1, l2, l3), d2 - 0.01)
        );

        Assert.assertEquals(
                List.of(l2, l1),
                GroupByLongestLegsId.findLegsByLimit(List.of(l1, l2, l3), d2 + 0.01)
        );

        Assert.assertEquals(
                List.of(l2, l1),
                GroupByLongestLegsId.findLegsByLimit(List.of(l1, l2, l3), d1 + d2 - 0.01)
        );

        Assert.assertEquals(
                List.of(l2, l1, l3),
                GroupByLongestLegsId.findLegsByLimit(List.of(l1, l2, l3), d1 + d2 + 0.01)
        );
    }

    @Test
    public void groupByLongestLegsMatches() {
        Itinerary i1 = newItinerary(A)
            .bus(21, 0, 5, B)
            .bus(31, 6, 18, C)
            .bus(41, 19, 22, D)
            .build();

        Itinerary i2 = newItinerary(A)
            .bus(21, 0, 5, B)
            .bus(31, 6, 18, C)
            .bus(41, 19, 22, E)
            .bus(51, 23,26, F)
            .build();

        Itinerary i3 = newItinerary(A)
            .bus(21, 0, 5, B)
            .bus(31, 6, 23, C)
            .build();

        GroupByLongestLegsId g1 = new GroupByLongestLegsId(i1, 0.6);
        GroupByLongestLegsId g2 = new GroupByLongestLegsId(i2, 0.6);
        GroupByLongestLegsId g3 = new GroupByLongestLegsId(i3, 0.6);

        Assert.assertTrue(g1.match(g1));
        Assert.assertTrue(g1.match(g2));
        Assert.assertTrue(g2.match(g1));
        Assert.assertFalse(g1.match(g3));
        Assert.assertFalse(g3.match(g1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalRangeForP() {
        new GroupByLongestLegsId(newItinerary(A).bus(21, 1, 2, B).build(), 0.91);
    }
}