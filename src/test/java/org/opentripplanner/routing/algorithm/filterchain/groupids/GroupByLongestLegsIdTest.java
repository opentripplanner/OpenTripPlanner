package org.opentripplanner.routing.algorithm.filterchain.groupids;

import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.List;

import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.A;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.B;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.C;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.D;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.E;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.F;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.itinerary;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.leg;

public class GroupByLongestLegsIdTest {
    @Test
    public void filterTransitLegs() {
        Leg l1 = leg(A, B, 1, 2, 12, TraverseMode.WALK);
        Leg l2 = leg(B, C, 3, 4, 5, TraverseMode.TRANSIT);

        List<Leg> legs = GroupByLongestLegsId.filterTransitLegs(List.of(l1, l2));

        Assert.assertEquals(List.of(l2), legs);
    }


    @Test
    public void calculateTotalDistance() {
        Leg l1 = leg(A, B, 3, 4, 5, TraverseMode.TRANSIT);
        Leg l2 = leg(B, C, 1, 2, 12, TraverseMode.TRANSIT);

        Assert.assertEquals(
                17.0,
                GroupByLongestLegsId.calculateTotalDistance(List.of(l1, l2)),
                0.0001
        );
    }

    @Test
    public void findLegsByLimit() {
        Leg l1 = leg(A, B, 3, 4, 5, TraverseMode.TRANSIT);
        Leg l2 = leg(B, C, 1, 2, 12, TraverseMode.TRANSIT);
        Leg l3 = leg(C, D, 5, 6, 3, TraverseMode.TRANSIT);

        Assert.assertEquals(
                List.of(l2),
                GroupByLongestLegsId.findLegsByLimit(List.of(l1, l2, l3), 11.99)
        );

        Assert.assertEquals(
                List.of(l2, l1),
                GroupByLongestLegsId.findLegsByLimit(List.of(l1, l2, l3), 12.01)
        );

        Assert.assertEquals(
                List.of(l2, l1),
                GroupByLongestLegsId.findLegsByLimit(List.of(l1, l2, l3), 16.99)
        );

        Assert.assertEquals(
                List.of(l2, l1, l3),
                GroupByLongestLegsId.findLegsByLimit(List.of(l1, l2, l3), 17.01)
        );
    }

    @Test
    public void groupByLongestLegsMatches() {
        Leg l1 = leg(A, B, 1, 2, 5, TraverseMode.TRANSIT);
        Leg l2 = leg(B, C, 3, 4, 12, TraverseMode.TRANSIT);
        Leg l2c = leg(B, D, 3, 5, 17, TraverseMode.TRANSIT);
        Leg l3a = leg(C, D, 5, 6, 3, TraverseMode.TRANSIT);
        Leg l3b = leg(C, E, 5, 6, 3, TraverseMode.TRANSIT);
        Leg l4b = leg(E, F, 7, 8, 3, TraverseMode.TRANSIT);

        GroupByLongestLegsId g1 = new GroupByLongestLegsId(itinerary(l1, l2, l3a), 0.6);
        GroupByLongestLegsId g2 = new GroupByLongestLegsId(itinerary(l1, l2, l3b, l4b), 0.6);
        GroupByLongestLegsId g3 = new GroupByLongestLegsId(itinerary(l1, l2c), 0.6);

        Assert.assertTrue(g1.match(g1));
        Assert.assertTrue(g1.match(g2));
        Assert.assertTrue(g2.match(g1));
        Assert.assertFalse(g1.match(g3));
        Assert.assertFalse(g3.match(g1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalRangeForP() {
        new GroupByLongestLegsId(new Itinerary(List.of(leg(A, B, 1, 2, 5, TraverseMode.TRANSIT))), 0.91);
    }
}