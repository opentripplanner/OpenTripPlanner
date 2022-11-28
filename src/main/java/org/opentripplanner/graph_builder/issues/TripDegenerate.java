package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.timetable.Trip;

public record TripDegenerate(Trip trip) implements DataImportIssue {
  public static final String FMT =
    "Trip %s has fewer than two stops. " +
    "We will not use it for routing. This is probably an error in your data";

  @Override
  public String getMessage() {
    return String.format(FMT, trip);
  }
}
