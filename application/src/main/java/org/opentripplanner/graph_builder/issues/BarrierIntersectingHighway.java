package org.opentripplanner.graph_builder.issues;

import org.apache.commons.text.StringEscapeUtils;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.osm.model.OsmNode;

public record BarrierIntersectingHighway(OsmNode node) implements DataImportIssue {
  private static final String FMT =
    "Node %d is the intersection of a linear barrier and a linear highway but it doesn't have barrier tags on it. According to the OpenStreetMap Wiki, such mapping is ambiguous. Please check if traversal is possible across the barrier and add the appropriate tags on the node.";

  private static final String HTMLFMT =
    "<a href='%s'>Node %d</a> is the intersection of a linear barrier and a linear highway but it doesn't have barrier tags on it. According to the <a href='https://wiki.openstreetmap.org/wiki/Barriers'>OpenStreetMap Wiki</a>, such mapping is ambiguous. Please check if traversal is possible across the barrier and add the appropriate tags on the node.";

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, StringEscapeUtils.escapeHtml4(node.url()), node.getId());
  }

  @Override
  public String getMessage() {
    return String.format(FMT, node.getId());
  }

  @Override
  public Geometry getGeometry() {
    return GeometryUtils.getGeometryFactory().createPoint(node.getCoordinate());
  }
}
