package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public record TooManyAreasInRelation(long relationId) implements DataImportIssue {
  private static String FMT = "Too many areas in relation %s";
  private static String HTMLFMT =
    "Too many areas in relation <a href='http://www.openstreetmap.org/relation/%s'>'%s'</a>";

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, relationId, relationId);
  }

  @Override
  public String getMessage() {
    return String.format(FMT, relationId);
  }
}
