package org.opentripplanner.graph_builder.issues;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.time.DurationUtils;

public record HopZeroDistance(
  int sec,
  Trip trip,
  int seq,
  StopLocation fromStop,
  StopLocation toStop
)
  implements DataImportIssue {
  private static final String FMT =
    "Zero-distance hop in %s on trip %s stop sequence %d between %s and %s.";

  @Override
  public String getMessage() {
    return String.format(FMT, DurationUtils.durationToStr(sec), trip, seq, fromStop, toStop);
  }

  @Override
  public int getPriority() {
    return sec;
  }

  @Override
  public Geometry getGeometry() {
    return toStop.getGeometry();
  }
}
