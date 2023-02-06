package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.street.model.edge.Edge;

public record ElevationProfileFailure(Edge edge, String reason) implements DataImportIssue {
  private static String FMT = "Failed to set elevation profile for %s: %s";

  @Override
  public String getMessage() {
    return String.format(FMT, edge, reason);
  }
}
