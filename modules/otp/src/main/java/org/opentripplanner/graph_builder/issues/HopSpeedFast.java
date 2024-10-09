package org.opentripplanner.graph_builder.issues;

import java.util.List;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;

public record HopSpeedFast(
  float metersPerSecond,
  float distance,
  Trip trip,
  int seq,
  StopLocation fromStop,
  StopLocation toStop
)
  implements DataImportIssue {
  private static final String FMT =
    "Excessive speed of %d kph over %.1fm on route %s trip %s " +
    "stop sequence %d between %s and %s.";

  @Override
  public String getMessage() {
    int kph = (int) (3.6 * metersPerSecond); // convert meters per second to kph
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
    return (int) metersPerSecond;
  }

  @Override
  public Geometry getGeometry() {
    return GeometryUtils.makeLineString(
      List.of(fromStop.getCoordinate().asJtsCoordinate(), toStop.getCoordinate().asJtsCoordinate())
    );
  }
}
