package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.timetable.Trip;

public record HopZeroDistance(int sec, Trip trip, int seq) implements DataImportIssue {
  private static String FMT = "Zero-distance hop in %s on trip %s stop sequence %d.";

  @Override
  public String getMessage() {
    return String.format(FMT, DurationUtils.durationToStr(sec), trip, seq);
  }

  @Override
  public int getPriority() {
    return sec;
  }
}
