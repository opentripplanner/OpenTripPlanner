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

    String   service_id;
    Calendar calendar;
    Map<DateTime, CalendarDate> calendar_dates = Maps.newHashMap();

    public Service(String service_id) {
        this.service_id = service_id;
    }

}
