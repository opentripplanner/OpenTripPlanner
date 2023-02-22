package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public record PublicTransportRelationSkipped(OSMWithTags entity) implements DataImportIssue {
  private static final String FMT = "Unable to process public transportation relation %s";
  private static final String HTMLFMT =
    "Unable to process public transportation relation <a href='%s'>'%s'</a>";

  @Override
  public String getMessage() {
    return String.format(FMT, entity.getId());
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, entity.getOpenStreetMapLink(), entity.getId());
  }
}
