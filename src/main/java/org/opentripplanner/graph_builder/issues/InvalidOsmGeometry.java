package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public record InvalidOsmGeometry(OSMWithTags entity) implements DataImportIssue {
  private static String FMT = "Invalid OSM geometry %s";
  private static String HTMLFMT = "Invalid OSM geometry <a href='%s'>'%s'</a>";

  @Override
  public String getMessage() {
    return String.format(FMT, entity.getId());
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, entity.getOpenStreetMapLink(), entity.getId());
  }
}
