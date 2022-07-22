package org.opentripplanner.routing.algorithm.filterchain.groupids;

import java.util.List;
import java.util.stream.Stream;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StationElement;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * This creates a group identifier based on all origin and destination stations, or stops if there
 * is no parent station, of all legs in the itinerary.
 * <p>
 * This is used to group itineraries that are almost the same, but where one might have a slight
 * time advantage and the other a slight cost advantage eg. due to shorter walking distance inside
 * the station.
 */
public class GroupBySameRoutesAndStops implements GroupId<GroupBySameRoutesAndStops> {

  public static final String TAG = "group-by-same-stations-and-routes";
  private final List<FeedScopedId> keySet;

  public GroupBySameRoutesAndStops(Itinerary itinerary) {
    keySet =
      itinerary
        .getLegs()
        .stream()
        .filter(Leg::isTransitLeg)
        .flatMap(leg ->
          Stream.of(
            getStopOrStationId(leg.getFrom().stop),
            leg.getRoute().getId(),
            getStopOrStationId(leg.getTo().stop)
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

  /**
   * Get the parent station id if such exists. Otherwise, return the stop id.
   */
  private static FeedScopedId getStopOrStationId(StopLocation stopPlace) {
    if (stopPlace instanceof StationElement stationElement && stationElement.isPartOfStation()) {
      return stationElement.getParentStation().getId();
    }
    return stopPlace.getId();
  }
}
