package org.opentripplanner.model.plan;


import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * An Itinerary is one complete way of getting from the start location to the end location.
 */
public class Itinerary {

    /**
     *  Total duration of the itinerary in seconds.
     */
    public Long durationSeconds = 0L;

    /**
     * How much time is spent on transit, in seconds.
     */
    public long transitTimeSeconds = 0;

    /**
     * The number of transfers this trip has.
     */
    public Integer nTransfers = 0;

    /**
     * How much time is spent waiting for transit to arrive, in seconds.
     */
    public long waitingTimeSeconds = 0;

    /**
     * How much time is spent walking/biking/driving, in seconds.
     */
    public long nonTransitTimeSeconds = 0;

    /**
     * How far the user has to walk, in meters.
     */
    public Double nonTransitDistanceMeters = 0.0;

    /**
     * Indicates that the walk limit distance has been exceeded for this itinerary when true.
     */
    public boolean nonTransitLimitExceeded = false;

    /**
     * How much elevation is lost, in total, over the course of the trip, in meters. As an example,
     * a trip that went from the top of Mount Everest straight down to sea level, then back up K2,
     * then back down again would have an elevationLost of Everest + K2.
     */

    public Double elevationLost = 0.0;
    /**
     * How much elevation is gained, in total, over the course of the trip, in meters. See
     * elevationLost.
     */

    public Double elevationGained = 0.0;

    /**
     * This itinerary has a greater slope than the user requested (but there are no possible
     * itineraries with a good slope).
     */
    public boolean tooSloped = false;

    /**
     * The cost of this trip
     */
    public Fare fare = new Fare();

    /**
     * A list of Legs. Each Leg is either a walking (cycling, car) portion of the trip, or a transit
     * trip on a particular vehicle. So a trip where the use walks to the Q train, transfers to the
     * 6, then walks to their destination, has four legs.
     */
    public List<Leg> legs = new ArrayList<>();

    /**
     * Time that the trip departs.
     */
    public Calendar startTime() {
        return firstLeg().startTime;
    }

    /**
     * Time that the trip arrives.
     */
    public Calendar endTime() {
        return lastLeg().endTime;
    }

    /**
     * Return {@code true} if all legs are WALKING.
     */
    public boolean isWalkingAllTheWay() {
        // We should have only one leg, but it is NOT the job of the itinerary to enforce that;
        // Hence, we iterate over all legs.
        return legs.stream().allMatch(it -> TraverseMode.WALK.toString().equals(it.mode));
    }

    public Leg firstLeg() {
        return legs.get(0);
    }

    public Leg lastLeg() {
        return legs.get(legs.size()-1);
    }

    /**
     * adds leg to array list
     */
    public void addLeg(Leg leg) {
        if(leg != null)
            legs.add(leg);
    }

    @Override
    public String toString() {
        return "Itinerary{"
                + "nTransfers=" + nTransfers
                + ", durationSeconds=" + durationSeconds
                + ", nonTransitTimeSeconds=" + nonTransitTimeSeconds
                + ", transitTimeSeconds=" + transitTimeSeconds
                + ", waitingTimeSeconds=" + waitingTimeSeconds
                + ", nonTransitDistanceMeters=" + nonTransitDistanceMeters
                + ", nonTransitLimitExceeded=" + nonTransitLimitExceeded
                + ", tooSloped=" + tooSloped
                + ", elevationLost=" + elevationLost
                + ", elevationGained=" + elevationGained
                + ", legs=" + legs
                + ", fare=" + fare
                + '}';
    }
}
