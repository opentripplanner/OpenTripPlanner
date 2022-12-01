package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.street.model.vertex.Vertex;

public class GraphIsland implements DataImportIssue {

  public static final String FMT =
    "Processed disconnected subgraph containing vertex '%s' at (%f, %f), with %d edges";
  public static final String HTMLFMT =
    "Processed disconnected subgraph containing vertex <a href='http://www.openstreetmap.org/node/%s'>'%s'</a>, with %d edges";

  final Vertex vertex;
  final int size;

  public GraphIsland(Vertex vertex, int size) {
    this.vertex = vertex;
    this.size = size;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, vertex, vertex.getCoordinate().x, vertex.getCoordinate().y, size);
  }

  @Override
  public String getHTMLMessage() {
    String label = vertex.getLabel();
    if (label.startsWith("osm:")) {
      String osmNodeId = label.split(":")[2];
      return String.format(HTMLFMT, osmNodeId, osmNodeId, size);
    } else {
      return this.getMessage();
    }
  }

  @Override
  public Vertex getReferencedVertex() {
    return vertex;
  }
}
