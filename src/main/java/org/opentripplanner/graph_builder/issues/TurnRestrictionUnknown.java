package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class TurnRestrictionUnknown implements DataImportIssue {

  public static final String FMT = "Invalid turn restriction tag %s in turn restriction %d";

  public static final String HTMLFMT =
    "Invalid turn restriction tag %s in  <a href=\"http://www.openstreetmap.org/relation/%d\">\"%d\"</a>";

  final String tagval;
  final long relationId;

  public TurnRestrictionUnknown(long relationId, String tagval) {
    this.relationId = relationId;
    this.tagval = tagval;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, tagval, relationId);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(FMT, tagval, relationId, relationId);
  }
}
