package org.opentripplanner.graph_builder.module.islandpruning;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexLabel;

public record GraphIsland(
  Subgraph island,
  int nothru,
  int restricted,
  int removed,
  String traversalMode
)
  implements DataImportIssue {
  private static final String FMT =
    "Pruned %s subgraph %s containing vertex '%s' at (%f, %f) of %d street vertices and %d stops. Edge changes: %d to nothru, %d to no traversal, %d erased";
  private static final String HTMLFMT =
    "Pruned %s <a href='http://www.openstreetmap.org/node/%s'>subgraph %s</a> of %d street vertices %d stops. Edge changes: %d to nothru, %d to no traversal, %d erased";

  @Override
  public String getType() {
    return "GraphIsland" + traversalMode + (nothru != 0 ? "NoThroughTraffic" : "");
  }

  @Override
  public String getMessage() {
    Vertex vertex = island.getRepresentativeVertex();
    return String.format(
      FMT,
      traversalMode,
      vertex.getLabel(),
      vertex,
      vertex.getCoordinate().x,
      vertex.getCoordinate().y,
      island.streetSize(),
      island.stopSize(),
      this.nothru,
      this.restricted,
      this.removed
    );
  }

  @Override
  public String getHTMLMessage() {
    VertexLabel label = island.getRepresentativeVertex().getLabel();
    if (label instanceof VertexLabel.OsmNodeLabel osmLabel) {
      return String.format(
        HTMLFMT,
        traversalMode,
        osmLabel.nodeId(),
        osmLabel.nodeId(),
        island.streetSize(),
        island.stopSize(),
        this.nothru,
        this.restricted,
        this.removed
      );
    } else {
      return this.getMessage();
    }
  }

  @Override
  public int getPriority() {
    return island.streetSize() + island.stopSize();
  }

  @Override
  public Vertex getReferencedVertex() {
    return island.getRepresentativeVertex();
  }

  @Override
  public Geometry getGeometry() {
    return island.getGeometry();
  }
}
