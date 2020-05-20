package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

 /**
 * This filter will sort the itineraries in OTP default order according to the request
 * {@code arriveBy} flag.
 * <p>
 * The SORT ORDER for a "depart-after-search" is:
 * <ol>
 *     <li>WALKING before everything else</li>
 *     <li>Earliest arrival time first</li>
 *     <li>Latest departure time first</li>
 * </ol>
  * <p>
  * The SORT ORDER for a "arrive-by-search" is:
  * <ol>
  *     <li>WALKING before everything else</li>
  *     <li>Latest departure time first</li>
  *     <li>Earliest arrival time first</li>
  * </ol>
 * <p>
 * The filter do only sort the itineraries, no other modifications are done.
 */
public class OtpDefaultSortOrder implements ItineraryFilter {

    private final boolean arriveBy;

    private static final Comparator<Itinerary> DEPART_AFTER_COMPARATOR = (o1, o2) -> {
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

    private static final Comparator<Itinerary> ARRIVE_BY_COMPARATOR = (o1, o2) -> {
        // Put walking first - encourage healthy lifestyle
        boolean w1 = o1.isWalkingAllTheWay();
        boolean w2 = o2.isWalkingAllTheWay();

        if (w1 && !w2) { return -1; }
        if (!w1 && w2) { return 1; }

        // Sort on departure time, latest first
        int v = o2.startTime().compareTo(o1.startTime());
        if(v != 0) { return v; }

        // Sort on arrival time, earliest first
        return o1.endTime().compareTo(o2.endTime());
    };

    public OtpDefaultSortOrder(boolean arriveBy) {
        this.arriveBy = arriveBy;
    }


    @Override
    public String name() {
        return "otp-default-sort-order";
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        return itineraries.stream()
            .sorted(arriveBy ? ARRIVE_BY_COMPARATOR : DEPART_AFTER_COMPARATOR)
            .collect(Collectors.toList());
    }
}
