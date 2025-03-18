package org.opentripplanner.gtfs;

import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.calendar.CalendarServiceData;

public interface GtfsContext {
  String getFeedId();

  OtpTransitService getTransitService();

  CalendarServiceData getCalendarServiceData();
}
