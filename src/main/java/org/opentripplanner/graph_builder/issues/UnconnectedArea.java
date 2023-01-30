package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public record UnconnectedArea(OSMWithTags entity, String idList) implements DataImportIssue {
  private static String FMT = "Area %s has no connection to street network";
  private static String HTMLFMT = "Area <a href='%s'>'%s'</a> has no connection to street network";

  @Override
  public String getMessage() {
    return String.format(FMT, idList);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, entity.getOpenStreetMapLink(), idList);
  }
}
