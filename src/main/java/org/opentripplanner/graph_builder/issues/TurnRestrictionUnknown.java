package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public record TurnRestrictionUnknown(OSMWithTags entity, String tagval) implements DataImportIssue {
  private static String FMT = "Invalid turn restriction tag %s in turn restriction %d";
  private static String HTMLFMT = "Invalid turn restriction tag %s in <a href='%s'>'%s'</a>";

  @Override
  public String getMessage() {
    return String.format(FMT, tagval, entity.getId());
  }

  @Override
  public String getHTMLMessage() {
    return String.format(FMT, tagval, entity.getOpenStreetMapLink(), entity.getId());
  }
}
