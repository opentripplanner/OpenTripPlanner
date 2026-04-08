package org.opentripplanner.graph_builder.issues;

import org.apache.commons.text.StringEscapeUtils;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.street.geometry.GeometryUtils;

public record DifferentLevelsSharingBarrier(OsmNode node, int levels) implements DataImportIssue {
  private static final String FMT =
    "Node %d is a barrier node but ways / relations on %s different layers / levels are connected to it. Please check if the barrier actually blocks traversal, if not, please remove the node from the barrier.";

  private static final String HTMLFMT =
    "<a href='%s'>Node %d</a> is a barrier node but ways / relations on %s different layers / levels are connected to it. Please check if the barrier actually blocks traversal, if not, please remove the node from the barrier.";

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, StringEscapeUtils.escapeHtml4(node.url()), node.getId(), levels);
  }

  @Override
  public String getMessage() {
    return String.format(FMT, node.getId(), levels);
  }

  @Override
  public Geometry getGeometry() {
    return GeometryUtils.getGeometryFactory().createPoint(node.getCoordinate());
  }
}
