package org.opentripplanner.routing.algorithm.filterchain.framework.groupids;

import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.GroupId;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * This creates a group identifier based on all origin and destination stations, or stops if there
 * is no parent station, of all legs in the itinerary.
 * <p>
 * This is used to group itineraries that are almost the same, but where one might have a slight
 * time advantage and the other a slight cost advantage eg. due to shorter walking distance inside
 * the station.
 */
public class GroupByAllSameStations implements GroupId<GroupByAllSameStations> {

  private final List<FeedScopedIdPair> keySet;

  public GroupByAllSameStations(Itinerary itinerary) {
    keySet = itinerary
      .legs()
      .stream()
      .filter(Leg::isTransitLeg)
      .map(leg ->
        new FeedScopedIdPair(
          leg.from().stop.getStationOrStopId(),
          leg.to().stop.getStationOrStopId()
        )
      )
      .collect(Collectors.toList());
  }

  @Override
  public boolean match(GroupByAllSameStations other) {
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
  public GroupByAllSameStations merge(GroupByAllSameStations other) {
    return this;
  }

  private record FeedScopedIdPair(FeedScopedId id0, FeedScopedId id1) {}
}
