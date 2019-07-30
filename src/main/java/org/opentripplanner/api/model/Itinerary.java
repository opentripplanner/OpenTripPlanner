package org.opentripplanner.api.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.routing.core.Fare;

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
     * Indicates that the walk limit distance has been exceeded for this itinerary when true.
     */
    public boolean walkLimitExceeded = false;

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
     * The number of transfers this trip has.
     */
    public Integer transfers = 0;

    /**
     * The cost of this trip
     */
    public Fare fare = new Fare();

    /**
     * A list of Legs. Each Leg is either a walking (cycling, car) portion of the trip, or a transit
     * trip on a particular vehicle. So a trip where the use walks to the Q train, transfers to the
     * 6, then walks to their destination, has four legs.
     */
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
        if(leg != null)
            legs.add(leg);
    }

    /** 
     * remove the leg from the list of legs 
     * @param leg object to be removed
     */
    public void removeLeg(Leg leg) {
        if(leg != null) {
            legs.remove(leg);
        }
    }
    
    public void fixupDates(CalendarServiceData service) {
        TimeZone startTimeZone = null;
        TimeZone timeZone = null;
        Iterator<Leg> it = legs.iterator();
        while (it.hasNext()) {
            Leg leg = it.next();
            if (leg.agencyId == null) {
                if (timeZone != null) {
                    leg.setTimeZone(timeZone);
                }
            } else {
                timeZone = service.getTimeZoneForAgencyId(leg.agencyId);
                if (startTimeZone == null) {
                    startTimeZone = timeZone; 
                 }
            }
        }
        if (timeZone != null) {
            Calendar calendar = Calendar.getInstance(startTimeZone);
            calendar.setTime(startTime.getTime());
            startTime = calendar;
            // go back and set timezone for legs prior to first transit
            it = legs.iterator();
            while (it.hasNext()) {
                Leg leg = it.next();
                if (leg.agencyId == null) {
                    leg.setTimeZone(startTimeZone);
                } else {
                    break;
                }
            }
            calendar = Calendar.getInstance(timeZone);
            calendar.setTime(endTime.getTime());
            endTime = calendar;
        }
    }
}
