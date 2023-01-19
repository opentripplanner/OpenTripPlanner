package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class AreaTooComplicated implements DataImportIssue {

  public static final String FMT = "Area %s is too complicated (%s > %s)";
  public static final String HTMLFMT =
    "Area <a href='http://www.openstreetmap.org/%s'>'%s'</a> is too complicated (%s > %s)";

  final String areaId;
  final int nbNodes;
  final int maxAreaNodes;

  public AreaTooComplicated(String areaId, int nbNodes, int maxAreaNodes) {
    this.areaId = areaId;
    this.nbNodes = nbNodes;
    this.maxAreaNodes = maxAreaNodes;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, areaId, nbNodes, maxAreaNodes);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, areaId, areaId, nbNodes, maxAreaNodes);
  }

  @Override
  public int getPriority() {
    return nbNodes;
  }
}
