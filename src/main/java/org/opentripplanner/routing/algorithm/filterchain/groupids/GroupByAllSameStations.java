package org.opentripplanner.routing.algorithm.filterchain.groupids;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.StationElement;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This creates a group identifier based on all origin and destination stations, or stops if there
 * is no parent station, of all legs in the itinerary.
 *
 * This is used to group itineraries that are almost the same, but where one might have e slight
 * time advantage and the other a slight cost advantage eg. due to shorter walking distance inside
 * the station.
 */
public class GroupByAllSameStations implements GroupId<GroupByAllSameStations> {
    private final List<P2<FeedScopedId>> keySet;

    public GroupByAllSameStations(Itinerary itinerary) {
        keySet = itinerary.legs.stream()
                .filter(Leg::isTransitLeg)
                .map(leg -> new P2<>(getStopOrStationId(leg.getFrom().stop), getStopOrStationId(
                        leg.getTo().stop)))
                .collect(Collectors.toList());
    }

    @Override
    public boolean match(GroupByAllSameStations other) {
        if (this == other) { return true; }

        // Itineraries without transit is not filtered - they are considered different
        if(this.keySet.isEmpty() || other.keySet.isEmpty()) { return false; }

        return this.keySet.equals(other.keySet);
    }

    @Override
    public GroupByAllSameStations merge(GroupByAllSameStations other) {
        return this;
    }

    /**
     * Get the parent station id if such exists. Otherwise, return the stop id.
     */
    private static FeedScopedId getStopOrStationId(StopLocation stopPlace) {
        if (stopPlace instanceof StationElement && ((StationElement) stopPlace).isPartOfStation()) {
            return ((StationElement) stopPlace).getParentStation().getId();
        }
        return stopPlace.getId();
    }
}
