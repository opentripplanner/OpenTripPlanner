package org.opentripplanner.graph_builder.module.islandpruning;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.street.model.vertex.Vertex;

public record PrunedStopIsland(
  Subgraph island,
  int nothru,
  int restricted,
  int removed,
  String stopLabels
)
  implements DataImportIssue {
  private static final String FMT =
    "Unlinked stops from pruned walk subgraph %s of %d street vertices and %d stops %s. Edge changes: %d to nothru, %d to no walking, %d erased";

  private static final String HTMLFMT =
    "Unlinked stops from pruned walk <a href='http://www.openstreetmap.org/node/%s'>subgraph %s</a> of %d street vertices and %d stops %s. Edge changes: %d to nothru, %d to no walking, %d erased";

  @Override
  public String getMessage() {
    return String.format(
      FMT,
      island.getRepresentativeVertex().getLabel(),
      island.streetSize(),
      island.stopSize(),
      this.stopLabels,
      this.nothru,
      this.restricted,
      this.removed
    );
  }

  @Override
  public String getHTMLMessage() {
    String label = island.getRepresentativeVertex().getLabel();
    if (label.startsWith("osm:")) {
      String osmNodeId = label.split(":")[2];
      return String.format(
        HTMLFMT,
        osmNodeId,
        osmNodeId,
        island.streetSize(),
        island.stopSize(),
        this.stopLabels,
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
}
