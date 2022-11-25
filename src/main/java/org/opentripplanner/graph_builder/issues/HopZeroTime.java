package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.timetable.Trip;

public class HopZeroTime implements DataImportIssue {

  public static final String FMT = "Zero-time hop over %fm on route %s trip %s stop sequence %d.";

  final float dist;

  final Trip trip;

  final int seq;

  public HopZeroTime(float dist, Trip trip, int seq) {
    this.dist = dist;
    this.trip = trip;
    this.seq = seq;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, dist, trip.getRoute().getId(), trip.getId(), seq);
  }
}
