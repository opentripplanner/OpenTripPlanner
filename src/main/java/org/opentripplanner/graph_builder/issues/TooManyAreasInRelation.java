package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class TooManyAreasInRelation implements DataImportIssue {

  public static final String FMT = "Too many areas in relation %s";
  public static final String HTMLFMT =
    "Too many areas in relation <a href='http://www.openstreetmap.org/relation/%s'>'%s'</a>";

  final long relationId;

  public TooManyAreasInRelation(long relationId) {
    this.relationId = relationId;
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, relationId, relationId);
  }

  @Override
  public String getMessage() {
    return String.format(FMT, relationId);
  }
}
