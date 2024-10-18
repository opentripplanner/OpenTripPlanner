package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public record TurnRestrictionException(long nodeId, long wayId) implements DataImportIssue {
  private static final String FMT = "Turn restriction with bicycle exception at node %s from %s";

  private static final String HTMLFMT =
    "Turn restriction with bicycle exception at node <a href=\"http://www.openstreetmap.org/node/%d\">\"%d\"</a> from <a href=\"http://www.openstreetmap.org/way/%d\">\"%d\"</a>";

  @Override
  public String getMessage() {
    return String.format(FMT, nodeId, wayId);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, nodeId, nodeId, wayId, wayId);
  }
}
