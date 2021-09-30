package org.opentripplanner.routing.algorithm.filterchain.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.opentripplanner.routing.algorithm.filterchain.tagger.MaxLimitFilter;
import org.opentripplanner.routing.algorithm.filterchain.groupids.GroupId;


/**
 * This filter groups the itineraries using a group-id and filter each group
 * by the given {@code filter}. It ensure that {@code maxNumberOfItinerariesPrGroup} requirement
 * is meat by reducing each group down to the given limit.
 *
 * @see GroupId on how to group itineraries
 */
public class GroupByFilter<T extends GroupId<T>> implements ItineraryListFilter {

    private final String name;
    private final int maxNumberOfItinerariesPrGroup;
    private final Function<Itinerary, T> groupingBy;
    private final ItineraryListFilter arrangeBy;

    public GroupByFilter(
            String name,
            Function<Itinerary, T> groupingBy,
            ItineraryListFilter arrangeBy,
            int maxNumberOfItinerariesPrGroup
    ) {
        this.name = name;
        this.groupingBy = groupingBy;
        this.arrangeBy = arrangeBy;
        this.maxNumberOfItinerariesPrGroup = maxNumberOfItinerariesPrGroup;
    }

    public final String name() {
        return name;
    }

    @Override
    public final List<Itinerary> filter(List<Itinerary> itineraries) {
        if(itineraries.size() <= maxNumberOfItinerariesPrGroup) { return itineraries; }

        List<Entry<T>> groups = new ArrayList<>();

        for (Itinerary it : itineraries) {
            T groupId = groupingBy.apply(it);
            Entry<T> matchFound = null;

            for (Entry<T> e : groups) {
                // ignore empty groups - they are merged into another group
                if(e.itineraries.isEmpty()) { continue; }

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

        final ItineraryListFilter maxLimitFilter = new FilteringFilter(
                new MaxLimitFilter(name(), maxNumberOfItinerariesPrGroup)
        );

        List<Itinerary> result = new ArrayList<>();
        for (Entry<T> e : groups) {
            List<Itinerary> groupResult = e.itineraries;
            groupResult = arrangeBy.filter(groupResult);
            groupResult = maxLimitFilter.filter(groupResult);
            result.addAll(groupResult);
        }
        return result;
    }

    private static class Entry<T extends GroupId<T>> {
        T groupId;
        List<Itinerary> itineraries = new ArrayList<>();

        Entry(T groupId, Itinerary it) {
            this.groupId = groupId;
            add(it);
        }

        void merge(T groupId, Itinerary itinerary) {
            this.groupId = this.groupId.merge(groupId);
            itineraries.add(itinerary);
        }

        void merge(Entry<T> other) {
            this.groupId = this.groupId.merge(other.groupId);
            this.itineraries.addAll(other.itineraries);
            other.itineraries.clear();
        }

        boolean match(T groupId) {
            return this.groupId.match(groupId);
        }

        void add(Itinerary it) {

            itineraries.add(it);
        }
    }
}
