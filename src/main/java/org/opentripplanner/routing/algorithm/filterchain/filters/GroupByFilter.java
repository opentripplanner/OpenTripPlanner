package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.routing.algorithm.filterchain.groupids.GroupId;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


/**
 * This filter group the itineraries using a group-id and filter each group
 * by the given {@code filter}. It ensure that {@code minLimit} requirement
 * is meat by limiting each group so the total become less than the min-limit.
 *
 * @see GroupId on how to group itineraries
 */
public class GroupByFilter<T extends GroupId<T>> implements ItineraryFilter {

    private final String name;
    private final int minLimit;
    private final Function<Itinerary, T> groupingBy;
    private final ItineraryFilter arrangeBy;

    public GroupByFilter(
            String name,
            Function<Itinerary, T> groupingBy,
            ItineraryFilter arrangeBy,
            int minLimit
    ) {
        this.name = name;
        this.groupingBy = groupingBy;
        this.arrangeBy = arrangeBy;
        this.minLimit = minLimit;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        if(itineraries.size() <= minLimit) { return itineraries; }

        List<Entry<T>> groups = new ArrayList<>();

        for (Itinerary it : itineraries) {
            T groupId = groupingBy.apply(it);
            Entry<T> matchFound = null;

            for (Entry<T> e : groups) {
                if (e.match(groupId)) {
                    if(matchFound == null) {
                        e.merge(groupId, it);
                        matchFound = e;
                    }
                    else {
                        matchFound.merge(e);
                    }
                }
            }
            if(matchFound == null) {
                groups.add(new Entry<>(groupId, it));
            }
        }
        // Remove leftover of group mergeAndClear operations
        groups.removeIf(g -> g.itineraries.isEmpty());

        final int groupMaxLimit = groupMaxLimit(minLimit, groups.size());
        final ItineraryFilter maxLimitFilter = new MaxLimitFilter(name(), groupMaxLimit);

        List<Itinerary> result = new ArrayList<>();
        for (Entry<T> e : groups) {
            List<Itinerary> groupResult = e.itineraries;
            groupResult = arrangeBy.filter(groupResult);
            groupResult = maxLimitFilter.filter(groupResult);
            result.addAll(groupResult);
        }
        return result;
    }

    @Override
    public boolean removeItineraries() {
        return true;
    }

    /**
     * Get a approximate max limit for each group so that the total
     * minLimit is respected. For example, if the min limit is 5 elements
     * and there is 3 groups, we set the maxLimit for each group to 2,
     * returning between 4 and 6 elements depending on the distribution.
     */
    static int groupMaxLimit(int minLimit, int nGroups) {

        return Math.max(1, Math.round((float) minLimit / nGroups));
    }

    private static class Entry<T extends GroupId<T>> {
        T groupId;
        List<Itinerary> itineraries = new ArrayList<>();

        Entry(T groupId, Itinerary it) {
            this.groupId = groupId;
            add(it);
        }

        void merge(T groupId, Itinerary itinerary) {
            mergeGroupId(groupId);
            itineraries.add(itinerary);
        }

        void merge(Entry<T> other) {
            mergeGroupId(other.groupId);
            itineraries.addAll(other.itineraries);
            other.itineraries.clear();
        }

        private void mergeGroupId(T groupId) {
            if (!this.groupId.match(groupId)) {
                throw new IllegalArgumentException("Not allowed to merge groups witch do not match.");
            }
            // If a new "higher" order group is found
            // switch to the highest order groupId
            if (!this.groupId.orderHigherOrEq(groupId)) {
                this.groupId = groupId;
            }
        }

        boolean match(T groupId) {
            return this.groupId.match(groupId);
        }

        void add(Itinerary it) {

            itineraries.add(it);
        }
    }
}
