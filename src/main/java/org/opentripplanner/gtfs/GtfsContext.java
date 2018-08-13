package org.opentripplanner.gtfs;

import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.CalendarService;
import org.opentripplanner.graph_builder.module.GtfsFeedId;

public interface GtfsContext {
    GtfsFeedId getFeedId();
    OtpTransitService getOtpTransitService();
    CalendarService getCalendarService();
}
