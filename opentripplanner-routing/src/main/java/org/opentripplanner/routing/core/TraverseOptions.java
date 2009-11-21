package org.opentripplanner.routing.core;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.gtfs.GtfsContext;

public class TraverseOptions {
    public double speed; // in meters/second

    public TraverseMode mode;

    public double transferPenalty = 600;

    public Calendar calendar;

    private CalendarService calendarService;

    private Map<AgencyAndId, Set<Date>> serviceDatesByServiceId = new HashMap<AgencyAndId, Set<Date>>();
    
    public TraverseOptions() {
        // http://en.wikipedia.org/wiki/Walking
        speed = 1.33; // 1.33 m/s ~ 3mph, avg. human speed
        mode = TraverseMode.WALK;
        calendar = Calendar.getInstance();
    }
    
    public TraverseOptions(TraverseMode mode) {
        this();
        this.mode = mode;
    }

    public TraverseOptions(GtfsContext context) {
        this();
        setGtfsContext(context);
    }

    public void setGtfsContext(GtfsContext context) {
        calendarService = context.getCalendarService();
    }
    
    public void setCalendarService(CalendarServiceImpl calendarService) {
        this.calendarService = calendarService;
    }
    
    public CalendarService getCalendarService() {
        return calendarService;
    }

    public boolean serviceOn(AgencyAndId serviceId, Date serviceDate) {
        Set<Date> dates = serviceDatesByServiceId.get(serviceId);
        if (dates == null) {
            dates = calendarService.getServiceDatesForServiceId(serviceId);
            serviceDatesByServiceId.put(serviceId, dates);
        }
        return dates.contains(serviceDate);
    }


}