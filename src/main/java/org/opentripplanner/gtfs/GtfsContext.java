package org.opentripplanner.gtfs;

import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.calendar.CalendarServiceData;

public interface GtfsContext {
  GtfsFeedId getFeedId();

  OtpTransitService getTransitService();

  CalendarServiceData getCalendarServiceData();
}
