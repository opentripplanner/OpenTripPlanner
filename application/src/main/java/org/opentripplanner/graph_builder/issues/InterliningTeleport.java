package org.opentripplanner.graph_builder.issues;

import java.util.List;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;

public record InterliningTeleport(
  Trip prevTrip,
  String blockId,
  int distance,
  StopLocation fromStop,
  StopLocation toStop
)
  implements DataImportIssue {
  private static final String FMT =
    "Interlining trip '%s' on block '%s' between %s and %s implies teleporting %d meters.";

  @Override
  public String getMessage() {
    return String.format(FMT, prevTrip, blockId, fromStop, toStop, distance);
  }

  @Override
  public int getPriority() {
    return distance;
  }

  @Override
  public Geometry getGeometry() {
    return GeometryUtils.makeLineString(
      List.of(fromStop.getCoordinate().asJtsCoordinate(), toStop.getCoordinate().asJtsCoordinate())
    );
  }
}
