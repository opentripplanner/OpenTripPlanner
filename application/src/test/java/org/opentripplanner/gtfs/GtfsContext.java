package org.opentripplanner.gtfs;

import org.opentripplanner.transit.model.calendar.TripCalendars;

public interface GtfsContext {
  String getFeedId();

  TripCalendars getTripCalendars();
}
