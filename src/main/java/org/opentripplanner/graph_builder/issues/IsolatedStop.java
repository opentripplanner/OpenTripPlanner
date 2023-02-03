package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.street.model.vertex.Vertex;

public record IsolatedStop(Vertex vertex) implements DataImportIssue {
  private static String FMT = "Unable to link stop %s to the street graph";

  @Override
  public String getMessage() {
    return String.format(FMT, vertex.getLabel());
  }

  @Override
  public Vertex getReferencedVertex() {
    return vertex;
  }
}
