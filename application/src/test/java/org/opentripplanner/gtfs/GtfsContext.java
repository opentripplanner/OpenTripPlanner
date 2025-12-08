package org.opentripplanner.gtfs;

import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;

public interface GtfsContext {
  String getFeedId();

  OtpTransitServiceBuilder getTransitService();

  CalendarServiceData getCalendarServiceData();
}
