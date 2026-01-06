package org.opentripplanner.gtfs;

import org.opentripplanner.model.calendar.CalendarServiceData;

public interface GtfsContext {
  String getFeedId();

  CalendarServiceData getCalendarServiceData();
}
