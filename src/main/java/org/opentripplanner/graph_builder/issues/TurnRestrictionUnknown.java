package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public record TurnRestrictionUnknown(long relationId, String tagval) implements DataImportIssue {
  private static String FMT = "Invalid turn restriction tag %s in turn restriction %d";
  private static String HTMLFMT =
    "Invalid turn restriction tag %s in  <a href=\"http://www.openstreetmap.org/relation/%d\">\"%d\"</a>";

  @Override
  public String getMessage() {
    return String.format(FMT, tagval, relationId);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(FMT, tagval, relationId, relationId);
  }
}
