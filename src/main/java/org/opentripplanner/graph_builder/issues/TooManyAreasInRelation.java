package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public record TooManyAreasInRelation(OSMWithTags entity) implements DataImportIssue {
  private static String FMT = "Too many areas in relation %s";
  private static String HTMLFMT = "Too many areas in relation  <a href='%s'>'%s'</a>";

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, entity.getOpenStreetMapLink(), entity.getId());
  }

  @Override
  public String getMessage() {
    return String.format(FMT, entity.getId());
  }
}
