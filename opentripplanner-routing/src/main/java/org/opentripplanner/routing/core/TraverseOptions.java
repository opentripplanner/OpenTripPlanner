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

    public long optimize_transfer_penalty = 1800; //by default, only transfer if it saves half an hour

    public double maxSlope = 0.0833333333333; //ADA max wheelchair ramp slope is a good default.

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