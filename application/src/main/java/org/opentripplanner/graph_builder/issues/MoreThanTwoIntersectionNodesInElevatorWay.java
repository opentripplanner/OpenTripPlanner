package org.opentripplanner.graph_builder.issues;

import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmWay;

public record MoreThanTwoIntersectionNodesInElevatorWay(
  OsmWay way,
  Coordinate from,
  Coordinate to,
  int intersectionNodes
)
  implements DataImportIssue {
  private static final String FMT =
    "Elevator way %s has more than two intersection nodes: %s. " +
    "This is likely a tagging mistake, but can be correct in rare cases. " +
    "Please check whether the elevator way is correctly modeled.";

  private static final String HTMLFMT =
    "<a href='%s'>Elevator way %s</a> has more than two intersection nodes: %s. " +
    "This is likely a tagging mistake, but can be correct in rare cases. " +
    "Please check whether the elevator way is correctly modeled.";

  @Override
  public String getMessage() {
    return String.format(FMT, way.getId(), intersectionNodes);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, way.url(), way.getId(), intersectionNodes);
  }

  @Override
  public Geometry getGeometry() {
    return GeometryUtils.makeLineString(List.of(from, to));
  }
}
