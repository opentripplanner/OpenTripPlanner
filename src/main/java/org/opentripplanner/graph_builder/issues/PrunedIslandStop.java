package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.OsmUrlGenerator;
import org.opentripplanner.street.model.vertex.Vertex;

public class PrunedIslandStop implements DataImportIssue {

  public static final String FMT =
    "Stop %s was linked to a pruned sub graph with %d edges and %d stops";

  final Vertex vertex;
  final int streetSize;
  final int stopSize;

  public PrunedIslandStop(Vertex vertex, int streetSize, int stopSize) {
    this.vertex = vertex;
    this.streetSize = streetSize;
    this.stopSize = stopSize;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, vertex.getLabel(), this.streetSize, this.stopSize);
  }

  @Override
  public String getHTMLMessage() {
    var url = OsmUrlGenerator.fromCoordinate(vertex.getCoordinate());
    return "<a href=\"%s\">%s</a>".formatted(url, getMessage());
  }

  @Override
  public int getPriority() {
    return streetSize + stopSize;
  }

  @Override
  public Vertex getReferencedVertex() {
    return vertex;
  }
}
