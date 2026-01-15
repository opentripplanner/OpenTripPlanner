package org.opentripplanner.graph_builder.issues;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmNode;

public record AllWaysOfElevatorNodeOnSameLevel(OsmNode node) implements DataImportIssue {
  private static final String FMT =
    "All ways connected to elevator node %s are on the same level. " +
    "Level information is parsed from the level or layer tag and defaults to 0 without tag data. " +
    "Please check whether the node is correctly modeled.";

  private static final String HTMLFMT =
    "All ways connected to <a href='%s'>elevator node %s</a> are on the same level. " +
    "Level information is parsed from the level or layer tag and defaults to 0 without tag data. " +
    "Please check whether the node is correctly modeled.";

  @Override
  public String getMessage() {
    return String.format(FMT, node.getId());
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, node.url(), node.getId());
  }

  @Override
  public Geometry getGeometry() {
    return GeometryUtils.getGeometryFactory().createPoint(node.getCoordinate());
  }
}
