package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public record PublicTransportRelationSkipped(long relationId) implements DataImportIssue {
  private static String FMT = "Unable to process public transportation relation %s";
  private static String HTMLFMT =
    "Unable to process public transportation relation <a href='http://www.openstreetmap.org/relation/%d'>'%d'</a>";

  @Override
  public String getMessage() {
    return String.format(FMT, relationId);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, relationId, relationId);
  }
}
