package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class AreaTooComplicated implements DataImportIssue {

  public static final String FMT = "Area %s is too complicated (%s > %s )";

  final long areaId;
  final int nbNodes;
  final int maxAreaNodes;

  public AreaTooComplicated(long areaId, int nbNodes, int maxAreaNodes) {
    this.areaId = areaId;
    this.nbNodes = nbNodes;
    this.maxAreaNodes = maxAreaNodes;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, areaId, nbNodes, maxAreaNodes);
  }
}
