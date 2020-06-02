package org.opentripplanner.routing.algorithm.filterchain.groupids;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class create a group identifier for an itinerary based on the set of the longest legs witch
 * together account for more than 'p' part of the total distance. The exact value of {@code p} is be
 * passed in to the constructor.
 * <p>
 * The legs are sorted on distance in decreasing order and added to the group id until they account
 * for more than {@code p} of the total distance.
 */
public class GroupByMainBodyOfLegsIds implements GroupId<GroupByMainBodyOfLegsIds> {
    private static final String NO_TRANSIT = "NO_TRANSIT";
    private final String groupId;

    public GroupByMainBodyOfLegsIds(Itinerary itinerary, int minFirstLastTripDurationInSeconds) {
        groupId = groupIdsAsString(itinerary, minFirstLastTripDurationInSeconds);
    }

    @Override
    public GroupByMainBodyOfLegsIds merge(GroupByMainBodyOfLegsIds other) {
        return this;
    }

    @Override
    public boolean match(GroupByMainBodyOfLegsIds other) {
        return this == other || groupId.equals(other.groupId);
    }

    /**
     * Create an id to group trips together. This method will concatenate the trips-ids for
     * all transit legs together. But, before doing that, it check the first and last trip
     * duration. If the duration is less then the {@code minFirstLastTripDurationInSeconds}
     * the first/last leg is excluded from the key. This ensure to kick out any short rides
     * int the start/end of the journey that is better on departure-time and/or arrival-time,
     * but not on cost.
     * <p>
     * Itineraries without any transit-legs longer than the {@code minFirstLastTripDurationInSeconds}
     * will be grouped together under the key {@link #NO_TRANSIT}.
     */
    private static String groupIdsAsString(Itinerary it, int minFirstLastTripDurationInSeconds) {
        List<Leg> transitLegs = it.legs.stream().filter(Leg::isTransitLeg).collect(Collectors.toList());
        Leg firstLeg, lastLeg;

        if(transitLegs.isEmpty()) {
            return NO_TRANSIT;
        }
        // Make trips with just one leg is not removed
        if(transitLegs.size() == 1) {
            return transitLegs.get(0).tripId.toString();
        }
        // Itineraries with 2 legs must handled carefully; If both legs are longer than the minLimit,
        // than both trip IDs are included in the key; If not the longest leg trip ID is used
        if(transitLegs.size() == 2) {
            firstLeg = transitLegs.get(0);
            lastLeg = transitLegs.get(1);

            if(firstLeg.getDuration() < lastLeg.getDuration()) {
                if(firstLeg.getDuration() < minFirstLastTripDurationInSeconds) {
                    return lastLeg.tripId.toString();
                }
            }
            else {
                if(lastLeg.getDuration() < minFirstLastTripDurationInSeconds) {
                    return firstLeg.tripId.toString();
                }
            }
            return firstLeg.tripId.toString() + ";" + lastLeg.tripId.toString();
        }
        // 3 or more legs
        if(transitLegs.get(0).getDuration() < minFirstLastTripDurationInSeconds) {
            transitLegs = transitLegs.subList(1, transitLegs.size());
        }
        if(transitLegs.get(transitLegs.size()-1).getDuration() < minFirstLastTripDurationInSeconds) {
            transitLegs = transitLegs.subList(0, transitLegs.size()-1);
        }
        // concatenate remaining trip isd
        return transitLegs.stream()
            .map(l -> l.tripId.toString())
            .collect(Collectors.joining(";"));
    }
}
