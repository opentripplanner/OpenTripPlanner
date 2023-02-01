package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.timetable.Trip;

public record HopSpeedSlow(float metersPerSecond, float distance, Trip trip, int seq)
  implements DataImportIssue {
  private static String FMT =
    "Very slow speed of %.2f kph over %.1fm on route %s trip %s " + "stop sequence %d.";

  @Override
  public String getMessage() {
    double kph = metersPerSecond * 3.6;
    return String.format(FMT, kph, distance, trip.getRoute().getId(), trip.getId(), seq);
  }

  @Override
  public int getPriority() {
    return (int) (metersPerSecond * -100);
  }
}
