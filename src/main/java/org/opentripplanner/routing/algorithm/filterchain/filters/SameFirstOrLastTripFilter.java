package org.opentripplanner.routing.algorithm.filterchain.filters;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.framework.groupids.GroupBySameFirstOrLastTrip;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.RemoveItineraryFlagger;

/**
 * This filter ensures that no more than one itinerary begins or ends with same trip.
 * It loops through itineraries from top to bottom. If itinerary matches with any other itinerary
 * from above, it is removed from list.
 * Uses {@link GroupBySameFirstOrLastTrip}.
 * for matching itineraries.
 */
public class SameFirstOrLastTripFilter implements RemoveItineraryFlagger {

  @Override
  public String name() {
    return "SameFirstOrLastTripFilter";
  }

  @Override
  public List<Itinerary> flagForRemoval(List<Itinerary> itineraries) {
    List<Itinerary> filtered = new ArrayList<>();
    List<GroupBySameFirstOrLastTrip> groups = new ArrayList<>();

    OUTER_LOOP:for (Itinerary it : itineraries) {
      GroupBySameFirstOrLastTrip currentGroup = new GroupBySameFirstOrLastTrip(it);

      for (GroupBySameFirstOrLastTrip group : groups) {
        if (group.match(currentGroup)) {
          filtered.add(it);
          continue OUTER_LOOP;
        }
      }

      groups.add(currentGroup);
    }

    return filtered;
  }
}
