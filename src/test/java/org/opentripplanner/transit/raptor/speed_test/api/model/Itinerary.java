package org.opentripplanner.transit.raptor.speed_test.api.model;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * An Itinerary is one complete way of getting from the start location to the end location.
 */
public class Itinerary {

    /**
     * Duration of the trip on this itinerary, in seconds.
     */
    public Long duration = 0L;

    /**
     * Time that the trip departs.
     */
    public Calendar startTime = null;
    /**
     * Time that the trip arrives.
     */
    public Calendar endTime = null;

    /**
     * How much time is spent walking, in seconds.
     */
    public long walkTime = 0;
    /**
     * How much time is spent on transit, in seconds.
     */
    public long transitTime = 0;
    /**
     * How much time is spent waiting for transit to arrive, in seconds.
     */
    public long waitingTime = 0;

    /**
     * How far the user has to walk, in meters.
     */
    public Double walkDistance = 0.0;

    /**
     * The number of transfers this trip has.
     */
    public Integer transfers = 0;


    /**
     * Weight of the itinerary, used for debugging
     */

    public double weight = 0;

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
}
