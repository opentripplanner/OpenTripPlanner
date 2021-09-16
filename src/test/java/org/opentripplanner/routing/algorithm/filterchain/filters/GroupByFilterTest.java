package org.opentripplanner.routing.algorithm.filterchain.filters;

import static org.junit.Assert.assertEquals;
import static org.opentripplanner.model.plan.Itinerary.toStr;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.algorithm.filterchain.groupids.GroupId;

public class GroupByFilterTest implements PlanTestConstants {

    /**
     * This test group by exact trip ids and test that the reduce function
     * works properly. It do not merge any groups.
     */
    @Test
    public void aSimpleTestGroupByMatchingTripIdsNoMerge() {
        List<Itinerary> result;

        // Group 1
        Itinerary i1 = newItinerary(A).bus(1, 0, 10, E).build();

        // Group 2, with 2 itineraries with the same id (no need to merge)
        Itinerary i2a = newItinerary(A).bus(2, 1, 11, E).build();
        Itinerary i2b = newItinerary(A).bus(2, 5, 16, E).build();

        List<Itinerary> all = List.of(i1, i2a, i2b);

        // With min Limit = 1, expect the best trips from both groups
        result = createFilter(1).filter(all);
        assertEquals(toStr(List.of(i1, i2a)), toStr(result));

        // With min Limit = 2, also one from each group
        result = createFilter(2).filter(all);
        assertEquals(toStr(List.of(i1, i2a, i2b)), toStr(result));

        // With min Limit = 3, we get all 3 itineraries
        result = createFilter(3).filter(all);
        assertEquals(toStr(List.of(i1, i2a, i2b)), toStr(result));
    }

    /**
     * This test group by trips where the trip ids share the same prefix. The purpose is to test
     * the merging of groups.
     */
    @Test
    public void testMerging() {
        List<Itinerary> result;

        // Given these 3 itineraries witch all should be grouped in the same group
        Itinerary i1 = newItinerary(A).bus(1, 1, 11, E).build();
        Itinerary i11 = newItinerary(A).bus(11, 5, 16, E).build();
        Itinerary i12 = newItinerary(A).bus(12, 5, 16, E).build();

        // Then, independent of the order they are processed, we expect the itineraries to
        // be grouped together into --ONE-- group. We do not have access to the groups,
        // so each group is reduced to one element: 'i1', which prove that all 3 itineraries where
        // grouped together. 'i11' and 'i12' do not match each other, while 'i1' match
        // both 'i11' and 'i12, so we use the following combination to test:
        List<Itinerary> inputA = List.of(i1, i11, i12);
        List<Itinerary> inputB = List.of(i11, i1, i12);
        List<Itinerary> inputC = List.of(i11, i12, i1);

        for (List<Itinerary> input : List.of(inputA, inputB, inputC)) {
            result = createFilter(1).filter(input);
            assertEquals(toStr(List.of(i1)), toStr(result));
        }
    }

    /**
     * Create a filter that group by the first leg trip-id, and uses the default sort for each
     * group.
     */
    private GroupByFilter<AGroupId> createFilter(int maxNumberOfItinerariesPrGroup) {
        return new GroupByFilter<>(
            "test", i -> new AGroupId(i.firstLeg().getTrip().getId().getId()),
            new OtpDefaultSortOrder(false),
            maxNumberOfItinerariesPrGroup
        );
    }

    /** A simple implementation of GroupId for this test */
    private static class AGroupId implements GroupId<AGroupId> {
        private final String id;
        public AGroupId(String id) { this.id = id; }

        @Override  public boolean match(AGroupId other) {
            return this.id.startsWith(other.id) || other.id.startsWith(this.id);
        }

        @Override public AGroupId merge(AGroupId other) {
            return this.id.length() <= other.id.length() ? this : other;
        }

        @Override public String toString() { return id; }
    }
}