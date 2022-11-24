package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.common.OsmUrlGenerator;
import org.opentripplanner.graph_builder.DataImportIssue;

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
