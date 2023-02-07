package org.opentripplanner.graph_builder.issues;

import java.util.List;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;

public record HopZeroTime(
  float dist,
  Trip trip,
  int seq,
  StopLocation fromStop,
  StopLocation toStop
)
  implements DataImportIssue {
  private static final String FMT =
    "Zero-time hop over %fm on route %s trip %s stop sequence %d between %s and %s.";

  @Override
  public String getMessage() {
    return String.format(FMT, dist, trip.getRoute().getId(), trip.getId(), seq, fromStop, toStop);
  }

  @Override
  public int getPriority() {
    return (int) dist;
  }

  @Override
  public Geometry getGeometry() {
    return GeometryUtils.makeLineString(
      List.of(fromStop.getCoordinate().asJtsCoordinate(), toStop.getCoordinate().asJtsCoordinate())
    );
  }
}
