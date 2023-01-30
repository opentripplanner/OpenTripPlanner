package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.timetable.Trip;

public record HopSpeedFast(float metersPerSecond, float distance, Trip trip, int seq)
  implements DataImportIssue {
  private static String FMT =
    "Excessive speed of %d kph over %.1fm on route %s trip %s " + "stop sequence %d.";

  @Override
  public String getMessage() {
    int kph = (int) (3.6 * metersPerSecond); // convert meters per second to kph
    return String.format(FMT, kph, distance, trip.getRoute().getId(), trip.getId(), seq);
  }

  @Override
  public int getPriority() {
    return (int) metersPerSecond;
  }
}
