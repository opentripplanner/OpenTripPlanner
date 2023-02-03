package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public record ConflictingBikeTags(OSMWithTags entity) implements DataImportIssue {
  private static String FMT =
    "Conflicting tags bicycle:[yes|designated] and cycleway:dismount, assuming dismount";
  private static String HTMLFMT =
    "Conflicting tags bicycle:[yes|designated] and cycleway:dismount on way <a href='%s'>'%s'</a>, assuming dismount";

  @Override
  public String getMessage() {
    return String.format(FMT);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, entity.getOpenStreetMapLink(), entity.getId());
  }
}
