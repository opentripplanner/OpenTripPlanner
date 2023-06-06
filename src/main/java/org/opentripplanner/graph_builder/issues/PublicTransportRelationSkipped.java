package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public record PublicTransportRelationSkipped(OSMWithTags entity) implements DataImportIssue {
  private static final String FMT = "Stop area relation %s has areas without link nodes";
  private static final String HTMLFMT =
    "Stop area relation <a href='%s'>'%s'</a> has areas without link nodes";

  @Override
  public String getMessage() {
    return String.format(FMT, entity.getId());
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, entity.getOpenStreetMapLink(), entity.getId());
  }
}
