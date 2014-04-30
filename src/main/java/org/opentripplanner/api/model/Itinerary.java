/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package org.opentripplanner.api.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
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
