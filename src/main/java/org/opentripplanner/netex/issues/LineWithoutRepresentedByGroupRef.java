package org.opentripplanner.netex.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class LineWithoutRepresentedByGroupRef implements DataImportIssue {
  public static final String fmt = "%s does not contain RepresentedByGroupRef";
  final String lineId;

  public LineWithoutRepresentedByGroupRef(String lineId) {
    this.lineId = lineId;
  }
  @Override
  public String getMessage() {
    return String.format(fmt, lineId);
  }
}
