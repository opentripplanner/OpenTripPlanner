package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.OsmUrlGenerator;
import org.opentripplanner.street.model.vertex.Vertex;

public class PrunedIslandStop implements DataImportIssue {

  public static final String FMT = "Stop %s was linked to a pruned sub graph";

  final Vertex vertex;

  public PrunedIslandStop(Vertex vertex) {
    this.vertex = vertex;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, vertex.getLabel());
  }

  @Override
  public String getHTMLMessage() {
    var url = OsmUrlGenerator.fromCoordinate(vertex.getCoordinate());
    return "<a href=\"%s\">%s</a>".formatted(url, getMessage());
  }

  @Override
  public Vertex getReferencedVertex() {
    return vertex;
  }
}
