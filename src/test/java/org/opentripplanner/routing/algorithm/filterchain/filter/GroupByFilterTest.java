package org.opentripplanner.routing.algorithm.filterchain.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.routing.algorithm.filterchain.comparator.SortOrderComparator;
import org.opentripplanner.routing.algorithm.filterchain.deletionflagger.MaxLimitFilter;
import org.opentripplanner.routing.algorithm.filterchain.groupids.GroupId;

public class GroupByFilterTest implements PlanTestConstants {

    /**
     * This test group by exact trip ids and test that the reduce function
     * works properly. It does not merge any groups.
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
        createFilter(1).filter(all);
        assertFalse(i1.isFlaggedForDeletion());
        assertFalse(i2a.isFlaggedForDeletion());
        assertTrue(i2b.isFlaggedForDeletion());

        // Remove notice after asserting
        i2b.systemNotices.remove(0);

        // With min Limit = 2, we get two from each group
        createFilter(2).filter(all);
        assertFalse(i1.isFlaggedForDeletion());
        assertFalse(i2a.isFlaggedForDeletion());
        assertFalse(i2b.isFlaggedForDeletion());

        // With min Limit = 3, we get all 3 itineraries
        createFilter(3).filter(all);
        assertFalse(i1.isFlaggedForDeletion());
        assertFalse(i2a.isFlaggedForDeletion());
        assertFalse(i2b.isFlaggedForDeletion());
    }

    /**
     * This test group by trips where the trip ids share the same prefix. The purpose is to test
     * the merging of groups.
     */
    @Test
    public void testMerging() {
        // Given these 3 itineraries which all should be grouped in the same group
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
            createFilter(1).filter(input);

            assertFalse(i1.isFlaggedForDeletion());

            // Remove notices after asserting
            assertTrue(i11.isFlaggedForDeletion());
            i11.systemNotices.remove(0);
            assertTrue(i12.isFlaggedForDeletion());
            i12.systemNotices.remove(0);
        }
    }

    /**
     * Create a filter that group by the first leg trip-id, and uses the default sort for each
     * group.
     */
    private GroupByFilter<AGroupId> createFilter(int maxNumberOfItinerariesPrGroup) {
        return new GroupByFilter<>(
            i -> new AGroupId(i.firstLeg().getTrip().getId().getId()),
            List.of(
                new SortingFilter(SortOrderComparator.defaultComparatorDepartAfter()),
                new DeletionFlaggingFilter(new MaxLimitFilter("test", maxNumberOfItinerariesPrGroup))
            )
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