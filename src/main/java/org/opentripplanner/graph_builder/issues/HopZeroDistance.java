package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.timetable.Trip;

public class HopZeroDistance implements DataImportIssue {

  public static final String FMT = "Zero-distance hop in %d seconds on trip %s stop sequence %d.";

  final int sec;
  final Trip trip;
  final int seq;

  public HopZeroDistance(int sec, Trip trip, int seq) {
    this.sec = sec;
    this.trip = trip;
    this.seq = seq;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, sec, trip, seq);
  }
}
