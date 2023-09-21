package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public record StreetCarSpeedZero(OSMWithTags entity) implements DataImportIssue {
  private static final String FMT = "Way %s has car speed zero";
  private static final String HTMLFMT = "Way <a href='%s'>'%s'</a> has car speed zero";

  @Override
  public String getMessage() {
    return String.format(FMT, entity.getId());
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, entity.url(), entity.getId());
  }
}
