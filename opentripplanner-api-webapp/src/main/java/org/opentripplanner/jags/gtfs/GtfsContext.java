package org.opentripplanner.jags.gtfs;

import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.onebusaway.gtfs.services.calendar.CalendarService;

public interface GtfsContext {
  public GtfsRelationalDao getDao();
  public CalendarService getCalendarService();
}
