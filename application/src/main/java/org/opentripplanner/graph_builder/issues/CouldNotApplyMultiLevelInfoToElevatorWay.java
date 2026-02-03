package org.opentripplanner.graph_builder.issues;

import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmWay;

public record CouldNotApplyMultiLevelInfoToElevatorWay(
  OsmWay way,
  Coordinate from,
  Coordinate to,
  int levels,
  int intersectionNodes
) implements DataImportIssue {
  private static final String FMT =
    "Multi-level info for elevator way %s can not be used. " +
    "The number of defined levels and intersection nodes did not match. " +
    "The way had %s defined levels and %s intersection nodes. " +
    "Check the level tag and how this way connects to other ways. " +
    "Falling back to default level for all elevator levels.";

  private static final String HTMLFMT =
    "Multi-level info for elevator <a href='%s'>way %s</a> can not be used. " +
    "The number of defined levels and intersection nodes did not match. " +
    "The way had %s defined levels and %s intersection nodes. " +
    "Check the level tag and how this way connects with other ways.";

  @Override
  public String getMessage() {
    return String.format(FMT, way.getId(), levels, intersectionNodes);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, way.url(), way.getId(), levels, intersectionNodes);
  }

  @Override
  public Geometry getGeometry() {
    return GeometryUtils.makeLineString(List.of(from, to));
  }
}
