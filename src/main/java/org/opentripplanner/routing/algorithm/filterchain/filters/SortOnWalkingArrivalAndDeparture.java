package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This filter SORT the itineraries:
 * <ol>
 *     <li>WALKING before everything else</li>
 *     <li>Earliest arrival time first</li>
 *     <li>Latest departure time first</li>
 * </ol>
 *
 * The filter do only sort the itineraries, no other modification is done.
 */
public class SortOnWalkingArrivalAndDeparture implements ItineraryFilter {

    private static final Comparator<Itinerary> COMPARATOR = (o1, o2) -> {
        // Put walking first - encourage healthy lifestyle
        boolean w1 = o1.isWalkingAllTheWay();
        boolean w2 = o2.isWalkingAllTheWay();

        if (w1 && !w2) { return -1; }
        if (!w1 && w2) { return 1; }

        // Sort on arrival time, earliest first
        int v = o1.endTime().compareTo(o2.endTime());
        if(v != 0) { return v; }

        // Sort on departure time, latest first
        return o2.startTime().compareTo(o1.startTime());
    };

    @Override
    public String name() {
        return "sort-on:walk-mode;arrival-time;departure-time";
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        return itineraries.stream().sorted(COMPARATOR).collect(Collectors.toList());
    }
}
