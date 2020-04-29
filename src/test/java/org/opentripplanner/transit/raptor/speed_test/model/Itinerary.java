package org.opentripplanner.transit.raptor.speed_test.model;


import org.opentripplanner.transit.raptor.util.PathStringBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * An Itinerary is one complete way of getting from the start location to the end location.
 */
public class Itinerary {
    private static final int NOT_SET = -1;

    /**
     * Duration of the trip on this itinerary, in seconds.
     */
    public int duration = NOT_SET;

    /**
     * Time that the trip departs.
     */
    public int startTime = NOT_SET;

    /**
     * Time that the trip arrives.
     */
    public int endTime = NOT_SET;

    /**
     * How much time is spent on transit, in seconds.
     */
    public long transitTime = NOT_SET;

    /**
     * How much time is spent waiting for transit to arrive, in seconds.
     */
    public long waitingTime = NOT_SET;

    /**
     * How far the user has to walk, in meters.
     */
    public Double walkDistance = 0.0;

    /**
     * The number of transfers this trip has.
     */
    public Integer transfers = NOT_SET;


    /**
     * Weight of the itinerary, used for debugging
     */

    public double weight = NOT_SET;

    /**
     * A list of Legs. Each Leg is either a walking (cycling, car) portion of the trip, or a transit
     * trip on a particular vehicle. So a trip where the use walks to the Q train, transfers to the
     * 6, then walks to their destination, has four legs.
     */
    public List<Leg> legs = new ArrayList<Leg>();

    /** adds leg to array list */
    public void addLeg(Leg leg) {
        if(leg != null) {
            if(leg.isWalkLeg()) {
                walkDistance += leg.distance;
            }
            legs.add(leg);
        }
    }

    @Override
    public String toString() {
        return details();
    }

    /**
     * Create a compact representation of all legs in the itinerary.
     * Example:
     * <pre>
     * WALK  7m12s ~ 3358 ~ NW180 09:30 10:20 ~ 3423 ~ WALK    10s ~ 8727 ~ NW130 10:30 10:40 ~ 3551 ~ WALK  3m10s
     * </pre>
     */
    public String details() {
        PathStringBuilder buf = new PathStringBuilder(true);

        for (Leg it : legs) {
            if(it.from != null && it.from.rrStopIndex >= 0) {
                buf.sep().stop(it.from.rrStopIndex).sep();
            }
            if(it.isWalkLeg()) {
                buf.walk(it.getDuration());
            }
            else if(it.isTransitLeg()) {
                buf.transit(it.mode.name(), it.startTime, it.endTime);
            }
        }
        return buf.toString();
    }
}
