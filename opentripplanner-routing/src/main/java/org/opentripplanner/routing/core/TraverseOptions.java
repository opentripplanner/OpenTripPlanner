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

package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.gtfs.GtfsContext;

public class TraverseOptions implements Serializable, Cloneable {

    private static final long serialVersionUID = 3836092451659658815L;

    /** max speed along streets, in meters per second */ 
    public double speed;

    public TraverseModeSet modes;

    public Calendar calendar;

    private CalendarService calendarService;

    private Map<AgencyAndId, Set<ServiceDate>> serviceDatesByServiceId = new HashMap<AgencyAndId, Set<ServiceDate>>();

    private boolean back = false;

    public boolean wheelchairAccessible = false;

    public OptimizeType optimizeFor = OptimizeType.QUICK;

    public double maxWalkDistance = Double.MAX_VALUE;

    private HashMap<AgencyAndId, FareRuleSet> fareContexts;

    /** When optimizing for few transfers, we don't actually optimize for fewest
     * transfers, as this can lead to absurd results.  Consider a trip in New York
     * from Grand Army Plaza (the one in Brooklyn) to Kalustyan's at noon.  The true 
     * lowest transfers route is to wait until midnight, when the 4 train runs local 
     * the whole way.  The actual fastest route is the 2/3 to the 4/5 at Nevins to 
     * the 6 at Union Square, which takes half an hour.  Even someone optimizing 
     * for fewest transfers doesn't want to wait until midnight.  Maybe they would  
     * be willing to walk to 7th Ave and take the Q to Union Square, then transfer 
     * to the 6.  If this takes less than optimize_transfer_penalty seconds, then 
     * that's what we'll return.
     */
    public long optimizeTransferPenalty = 1800; 

    public double maxSlope = 0.0833333333333; //ADA max wheelchair ramp slope is a good default.

    /** How much worse walking is than waiting for an equivalent length of time, as 
     * a multiplier.
     */
    public double walkReluctance = 2.0;

    /** This prevents unnecessary transfers by adding a cost for boarding a vehicle. */
    public int boardCost = 120; 

    /** Do not use certain named routes, probably because we're trying
     * to find alternate itineraries.
     */
    public HashSet<RouteSpec> bannedRoutes = new HashSet<RouteSpec>();

    /**
     * The worst possible time (latest for depart-by and earliest for arrive-by) that 
     * we will accept when planning a trip.
     */
    public long worstTime = Long.MAX_VALUE;

    /**
     * The worst possible weight that we will accept when planning a trip.
     */
    public double maxWeight = Double.MAX_VALUE;

    public int maxTransfers = 2;

    /*
     * How much less bad waiting at the beginning of the trip is
     */
    public double waitAtBeginningFactor = 0.1;
    
    public TraverseOptions() {
        // http://en.wikipedia.org/wiki/Walking
        speed = 1.33; // 1.33 m/s ~ 3mph, avg. human speed
        modes = new TraverseModeSet(new TraverseMode[]{TraverseMode.WALK, TraverseMode.TRANSIT});
        calendar = Calendar.getInstance();
    }
    
    public TraverseOptions(TraverseModeSet modes) {
        this();
        this.modes = modes;
        if (modes.getBicycle()) {
            speed = 5; //5 m/s, ~11 mph, a random bicycling speed.
            boardCost = 240; //cyclists hate loading their bike a second time
        }
    }

    public TraverseOptions(GtfsContext context) {
        this();
        setGtfsContext(context);
    }

    public void setGtfsContext(GtfsContext context) {
        calendarService = context.getCalendarService();
        fareContexts = context.getFareRules();
    }

    public void setCalendarService(CalendarServiceImpl calendarService) {
        this.calendarService = calendarService;
    }
    
    public CalendarService getCalendarService() {
        return calendarService;
    }

    public boolean serviceOn(AgencyAndId serviceId, ServiceDate serviceDate) {
        Set<ServiceDate> dates = serviceDatesByServiceId.get(serviceId);
        if (dates == null) {
            dates = calendarService.getServiceDatesForServiceId(serviceId);
            serviceDatesByServiceId.put(serviceId, dates);
        }
        return dates.contains(serviceDate);
    }

    public boolean transitAllowed() {
        return modes.getTransit();
    }

    /**
     * @return the fareContexts
     */
    public HashMap<AgencyAndId, FareRuleSet> getFareContexts() {
        return fareContexts;
    }

    @SuppressWarnings("unchecked")
    @Override
    public TraverseOptions clone() {
        try {
            TraverseOptions clone = (TraverseOptions) super.clone();
            clone.bannedRoutes = (HashSet<RouteSpec>) bannedRoutes.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            /* this will never happen since our super is the cloneable object */
            throw new RuntimeException(e); 
        }
    }
    
    public boolean equals(Object o) {
        if (o instanceof TraverseOptions) {
            TraverseOptions to = (TraverseOptions) o;
        return speed == to.speed && 
               maxWeight == to.maxWeight &&
               worstTime == to.worstTime &&
               modes.equals(to.modes) &&
               isArriveBy() == to.isArriveBy() &&
               wheelchairAccessible == to.wheelchairAccessible &&
               optimizeFor == to.optimizeFor &&
               maxWalkDistance == to.maxWalkDistance &&
               optimizeTransferPenalty == to.optimizeTransferPenalty && 
               maxSlope == to.maxSlope &&
               walkReluctance == to.walkReluctance && 
               boardCost == to.boardCost && 
               bannedRoutes.equals(to.bannedRoutes);
        }
        return false;
    }
    
    public int hashCode() {
        return new Double(speed).hashCode() + 
        new Double(maxWeight).hashCode() + 
        (int) (worstTime & 0xffffffff) +
        modes.hashCode() + 
        (isArriveBy() ? 8966786 : 0) +
        (wheelchairAccessible ? 731980 : 0) +
        optimizeFor.hashCode() + 
        new Double(maxWalkDistance).hashCode() +
        new Double(optimizeTransferPenalty).hashCode() + 
        new Double(maxSlope).hashCode() +
        new Double(walkReluctance).hashCode() + 
        boardCost + 
        bannedRoutes.hashCode();
    }

    public void setArriveBy(boolean back) {
        this.back = back;
        if (back) {
            this.worstTime = 0;
        } else {
            this.worstTime = Long.MAX_VALUE;
        }
    }

    public boolean isArriveBy() {
        return back;
    }

    public double distanceWalkFactor(double walkDistance) {
        if (walkDistance > maxWalkDistance && modes.getTransit()) {
            double weightFactor = (walkDistance - maxWalkDistance) / 20;
            return weightFactor < 1 ? 1 : weightFactor;
        }
        return 1;
    }
}