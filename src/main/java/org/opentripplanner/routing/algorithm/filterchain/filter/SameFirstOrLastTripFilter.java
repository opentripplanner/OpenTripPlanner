package org.opentripplanner.routing.algorithm.filterchain.filter;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;
import org.opentripplanner.routing.algorithm.filterchain.groupids.GroupBySameFirstOrLastTrip;

/**
 * This filter ensures that no more than one itinerary begins or ends with same trip.
 * It loops through itineraries from top to bottom. If itinerary matches with any other itinerary
 * from above, it is removed from list.
 * Uses {@link org.opentripplanner.routing.algorithm.filterchain.groupids.GroupBySameFirstOrLastTrip}.
 * for matching itineraries.
 */
public class SameFirstOrLastTripFilter implements ItineraryListFilter {

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    List<GroupBySameFirstOrLastTrip> groups = new ArrayList<>();

    OUTER_LOOP:for (Itinerary it : itineraries) {
      GroupBySameFirstOrLastTrip currentGroup = new GroupBySameFirstOrLastTrip(it);

      for (GroupBySameFirstOrLastTrip group : groups) {
        if (group.match(currentGroup)) {
          it.flagForDeletion(
            new SystemNotice("SameFirstOrLastTripFilter", "Deleted by SameFirstOrLastTripFilter")
          );
          continue OUTER_LOOP;
        }
      }

      groups.add(currentGroup);
    }

    return itineraries;
  }
}
