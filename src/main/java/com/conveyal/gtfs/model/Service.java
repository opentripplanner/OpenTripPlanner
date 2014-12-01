package com.conveyal.gtfs.model;

import com.google.common.collect.Maps;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * This table does not exist in GTFS. It is a join of calendars and calendar_dates on service_id.
 * There should only be one Calendar per service_id. There should only be one calendar_date per tuple of
 * (service_id, date), which means there can be many calendar_dates per service_id.
 */
public class Service {

    public String   service_id;
    public Calendar calendar;
    public Map<DateTime, CalendarDate> calendar_dates = Maps.newHashMap();

    public Service(String service_id) {
        this.service_id = service_id;
    }

    /**
     * Is this service active on the specified date?
     * @param date
     * @return
     */
    public boolean activeOn (DateTime date) {
        // first check for exceptions
        CalendarDate exception = calendar_dates.get(date);

        if (exception != null)
            return exception.exception_type == 1;

        else if (calendar == null)
            return false;

        else {
            int gtfsDate = date.getYear() * 10000 + date.getMonthOfYear() * 100 + date.getDayOfMonth(); 
            return calendar.end_date >= gtfsDate && calendar.start_date <= gtfsDate;
        }
    }
}
