package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class Graphwide implements DataImportIssue {

  String message;

  public Graphwide(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return "graph-wide: " + message;
  }
}
