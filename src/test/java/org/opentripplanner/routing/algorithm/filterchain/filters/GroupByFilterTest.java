package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.routing.algorithm.filterchain.groupids.GroupByLongestLegsId;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.A;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.B;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.C;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.D;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.E;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.F;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.itinerary;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.leg;
import static org.opentripplanner.routing.algorithm.filterchain.FilterChainTestData.toStr;
import static org.opentripplanner.routing.algorithm.filterchain.filters.GroupByFilter.groupMaxLimit;
import static org.opentripplanner.routing.core.TraverseMode.TRANSIT;
import static org.opentripplanner.routing.core.TraverseMode.WALK;

public class GroupByFilterTest {

    private GroupByFilter<GroupByLongestLegsId> createFilter(int minLimit) {
        return new GroupByFilter<>("test",
                i -> new GroupByLongestLegsId(i, .5),
                new OtpDefaultSortOrder(false),
                minLimit
        );
    }

    @Test
    public void groupByTheLongestItineraryAndTwoGroups() {
        GroupByFilter<GroupByLongestLegsId> subject;
        List<Itinerary> result;

        // Group 1
        Itinerary i1 = itinerary(leg(A, C, 6, 19, 9.0, WALK));

        // Group 2, with 3 itineraries
        Itinerary i2 = itinerary(
                leg(A, B, 6, 12, 8.0, TRANSIT),
                leg(B, C, 13, 14, 2.0, TRANSIT)
        );
        Itinerary i3 = itinerary(
                leg(A, B, 6, 12, 8.0, TRANSIT),
                leg(B, C, 13, 15, 1.0, TRANSIT)
        );

        List<Itinerary> list = List.of(i1, i2, i3);

        // With min Limit = 1, expect the best trips from both groups
        result = createFilter(1).filter(list);
        assertEquals(toStr(List.of(i1, i2)), toStr(result));

        // With min Limit = 2, also one from each group
        result = createFilter(2).filter(list);
        assertEquals(toStr(List.of(i1, i2)), toStr(result));

        // With min Limit = 3, still one from each group
        result = createFilter(3).filter(list);
        assertEquals(toStr(List.of(i1, i2, i3)), toStr(result));
    }

    @Test
    public void groupByTheLongestItineraryWithHigherOrderGroups() {
        List<Itinerary> result;

        Leg legAB = leg(A, B, 6, 12, 6.0, TRANSIT);
        Leg legBC = leg(B, C, 13, 14, 10.0, TRANSIT);

        // GroupId: [legBC, legAB]  Distance: 10 + 6 > 50% of total(6+10+5 = 21)
        Itinerary it1 = itinerary(legAB, legBC, leg(C, D, 14, 15, 5.0, TRANSIT));

        // GroupId: [legBC, legEF, legFD]  Distance: 10 + 9 + 8 > 50% of 6+10+7+9+8 = 40
        Itinerary it2 = itinerary(
                legAB,
                legBC,
                leg(C, E, 14, 16, 7.0, TRANSIT),
                leg(E, F, 17, 18, 9.0, TRANSIT),
                leg(F, D, 19, 20, 8.0, TRANSIT)
        );

        // GroupId: [legBC]  Distance: 10 > 50% of total(6+10+3 = 19)
        Itinerary it3 = itinerary(legAB, legBC, leg(C, D, 14, 30, 3.0, WALK));

        // GroupId: [legBC, legCD]  Distance: 10 + 9 > 50% of total(6+10+9 = 25)
        Itinerary it4 = itinerary(legAB, legBC, leg(C, D, 14, 18, 9.0, TRANSIT));

        // Expected order:
        String exp1 = toStr(List.of(it1));
        String exp2 = toStr(List.of(it1, it4));
        String exp4 = toStr(List.of(it1, it2, it3, it4));

        // Assert all itineraries is put in the same group - expect just one result back
        result = createFilter(1).filter(List.of(it1, it2, it3, it4));
        assertEquals(exp1, toStr(result));

        // Assert that we get all back if the limit is high enough
        result = createFilter(4).filter(List.of(it1, it2, it3, it4));
        assertEquals(exp4, toStr(result));

        // Assert that the itinerary order do not affect the results - test 2 itineraries is ok
        ItineraryFilter f = createFilter(2);
        assertEquals(exp2, toStr(f.filter(List.of(it2, it1, it3, it4))));
        assertEquals(exp2, toStr(f.filter(List.of(it3, it2, it1, it4))));
        assertEquals(exp2, toStr(f.filter(List.of(it4, it3, it2, it1))));
        assertEquals(exp2, toStr(f.filter(List.of(it3, it1, it4, it2))));
    }

    @Test
    public void testGroupMaxLimit() {
        // min limit = 1
        assertEquals(1, groupMaxLimit(1, 1));
        assertEquals(1, groupMaxLimit(1, 100));

        // min limit = 2
        assertEquals(2, groupMaxLimit(2, 1));
        assertEquals(1, groupMaxLimit(2, 2));
        assertEquals(1, groupMaxLimit(2, 100));

        // min limit = 3
        assertEquals(3, groupMaxLimit(3, 1));
        assertEquals(2, groupMaxLimit(3, 2));
        assertEquals(1, groupMaxLimit(3, 3));

        // min limit = 4
        assertEquals(4, groupMaxLimit(4, 1));
        assertEquals(2, groupMaxLimit(4, 2));
        assertEquals(1, groupMaxLimit(4, 3));
        assertEquals(1, groupMaxLimit(4, 100));

        // min limit = 5
        assertEquals(5, groupMaxLimit(5, 1));
        assertEquals(3, groupMaxLimit(5, 2));
        assertEquals(2, groupMaxLimit(5, 3));
        assertEquals(1, groupMaxLimit(5, 4));
        assertEquals(1, groupMaxLimit(5, 100));
    }
}