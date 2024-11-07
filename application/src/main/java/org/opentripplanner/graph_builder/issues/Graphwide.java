package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public record Graphwide(String message) implements DataImportIssue {
  @Override
  public String getMessage() {
    return "graph-wide: " + message;
  }
}
