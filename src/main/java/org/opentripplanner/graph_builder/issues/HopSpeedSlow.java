package org.opentripplanner.graph_builder.issues;

import java.util.List;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;

public record HopSpeedSlow(
  float metersPerSecond,
  float distance,
  Trip trip,
  int seq,
  StopLocation fromStop,
  StopLocation toStop
)
  implements DataImportIssue {
  private static final String FMT =
    "Very slow speed of %.2f kph over %.1fm on route %s trip %s " +
    "stop sequence %d between %s and %s.";

  @Override
  public String getMessage() {
    double kph = metersPerSecond * 3.6;
    return String.format(
      FMT,
      kph,
      distance,
      trip.getRoute().getId(),
      trip.getId(),
      seq,
      fromStop,
      toStop
    );
  }

  @Override
  public int getPriority() {
    return (int) (metersPerSecond * -100);
  }

  @Override
  public Geometry getGeometry() {
    return GeometryUtils.makeLineString(
      List.of(fromStop.getCoordinate().asJtsCoordinate(), toStop.getCoordinate().asJtsCoordinate())
    );
  }
}
