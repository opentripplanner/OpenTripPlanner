package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.timetable.Trip;

public record HopZeroTime(float dist, Trip trip, int seq) implements DataImportIssue {
  private static String FMT = "Zero-time hop over %fm on route %s trip %s stop sequence %d.";

  @Override
  public String getMessage() {
    return String.format(FMT, dist, trip.getRoute().getId(), trip.getId(), seq);
  }
}
