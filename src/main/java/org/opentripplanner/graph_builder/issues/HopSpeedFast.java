package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.timetable.Trip;

public class HopSpeedFast implements DataImportIssue {

  public static final String FMT =
    "Excessive speed of %d kph over %.1fm on route %s trip %s " + "stop sequence %d.";

  final float metersPerSecond;

  final float distance;

  final Trip trip;

  final int seq;

  public HopSpeedFast(float metersPerSecond, float distance, Trip trip, int seq) {
    this.metersPerSecond = metersPerSecond;
    this.distance = distance;
    this.trip = trip;
    this.seq = seq;
  }

  @Override
  public String getMessage() {
    int kph = (int) (3.6 * metersPerSecond); // convert meters per second to kph
    return String.format(FMT, kph, distance, trip.getRoute().getId(), trip.getId(), seq);
  }
}
