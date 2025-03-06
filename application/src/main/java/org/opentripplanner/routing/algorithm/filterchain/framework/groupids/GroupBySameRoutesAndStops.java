package org.opentripplanner.routing.algorithm.filterchain.framework.groupids;

import java.util.List;
import java.util.stream.Stream;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.GroupId;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * This creates a group identifier based on each transit leg's origin and destination stations/stops
 * and their routes.
 * <p>
 * This is useful if you want to see a wide variety of possible options rather than the ones with
 * the lowest cost.
 */
public class GroupBySameRoutesAndStops implements GroupId<GroupBySameRoutesAndStops> {

  public static final String TAG = "group-by-same-stations-and-routes";
  private final List<FeedScopedId> keySet;

  public GroupBySameRoutesAndStops(Itinerary itinerary) {
    keySet = itinerary
      .getLegs()
      .stream()
      .filter(Leg::isTransitLeg)
      .flatMap(leg ->
        Stream.of(
          leg.getFrom().stop.getStationOrStopId(),
          leg.getRoute().getId(),
          leg.getTo().stop.getStationOrStopId()
        )
      )
      .toList();
  }

  @Override
  public boolean match(GroupBySameRoutesAndStops other) {
    if (this == other) {
      return true;
    }

    // Itineraries without transit is not filtered - they are considered different
    if (this.keySet.isEmpty() || other.keySet.isEmpty()) {
      return false;
    }

    return this.keySet.equals(other.keySet);
  }

  @Override
  public GroupBySameRoutesAndStops merge(GroupBySameRoutesAndStops other) {
    return this;
  }
}
