package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public class TurnRestrictionBad implements DataImportIssue {

  public static final String FMT = "Bad turn restriction at relation %s. Reason: %s";
  public static final String HTMLFMT =
    "Bad turn restriction at relation <a href='http://www.openstreetmap.org/relation/%s'>%s</a>. Reason: %s";

  final long id;

  final String reason;

  public TurnRestrictionBad(long relationOSMID, String reason) {
    this.id = relationOSMID;
    this.reason = reason;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, id, reason);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, id, id, reason);
  }
}
