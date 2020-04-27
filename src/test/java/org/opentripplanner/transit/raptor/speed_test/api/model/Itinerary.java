package org.opentripplanner.transit.raptor.speed_test.api.model;


import org.opentripplanner.transit.raptor.util.TimeUtils;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
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
     * How much time is spent walking, in seconds.
     */
    public long walkTime = NOT_SET;
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
    @XmlElementWrapper(name = "legs")
    @XmlElement(name = "leg")
    public List<Leg> legs = new ArrayList<Leg>();

    /**
     * This itinerary has a greater slope than the user requested (but there are no possible 
     * itineraries with a good slope). 
     */
    public boolean tooSloped = false;

    /** 
     * adds leg to array list
     * @param leg
     */
    public void addLeg(Leg leg) {
        if(leg != null) {
            if(leg.isWalkLeg()) {
                walkDistance += leg.distance;
            }
            legs.add(leg);
        }
    }

    public String durationAsStr() {
        return TimeUtils.durationToStr(duration);
    }

    public String startTimeAsStr() {
        return TimeUtils.timeToStrCompact(startTime, NOT_SET);
    }

    public String endTimeAsStr() {
        return TimeUtils.timeToStrCompact(endTime, NOT_SET);
    }
}
