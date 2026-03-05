package org.opentripplanner.graph_builder.issues;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.street.geometry.GeometryUtils;

public record OnlyOneConnectionToElevatorNode(OsmNode node) implements DataImportIssue {
  private static final String FMT =
    "Elevator node %s has only one routable connection. " +
    "This makes the elevator unusable. " +
    "Please check whether the node is correctly modeled.";

  private static final String HTMLFMT =
    "<a href='%s'>Elevator node %s</a> has only one routable connection. " +
    "This makes the elevator unusable. " +
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
