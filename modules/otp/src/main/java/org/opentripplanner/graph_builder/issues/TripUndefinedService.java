package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.timetable.Trip;

public record TripUndefinedService(Trip trip) implements DataImportIssue {
  private static final String FMT =
    "Trip %s references serviceId %s that was not defined in the feed.";

  @Override
  public String getMessage() {
    return String.format(FMT, trip, trip.getServiceId());
  }
}
