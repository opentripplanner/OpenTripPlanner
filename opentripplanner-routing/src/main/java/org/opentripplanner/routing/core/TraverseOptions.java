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

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.gtfs.GtfsContext;

public class TraverseOptions {
    public double speed; // in meters/second

    public TraverseModeSet modes;

    public Calendar calendar;

    private CalendarService calendarService;

    private Map<AgencyAndId, Set<ServiceDate>> serviceDatesByServiceId = new HashMap<AgencyAndId, Set<ServiceDate>>();

    public boolean back = false;

    public boolean wheelchairAccessible = false;

    public OptimizeType optimizeFor;

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
    public long optimize_transfer_penalty = 1800; 

    public double maxSlope = 0.0833333333333; //ADA max wheelchair ramp slope is a good default.

    /** How much worse walking is than waiting for an equivalent length of time, as 
     * a multiplier.
     */
    public double walkReluctance = 1.1; 

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
            speed = 6; //6 m/s, ~13.5 mph, a random bicycling speed.
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


}