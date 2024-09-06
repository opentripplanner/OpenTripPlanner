package org.opentripplanner.openstreetmap.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public record FloorNumberUnknownAssumedGroundLevel(String layer, OSMWithTags entity)
  implements DataImportIssue {
  private static final String FMT =
    "%s : could not determine floor number for layer %s, assumed to be ground-level.";

  private static final String HTMLFMT =
    "<a href='%s'>'%s'</a> : could not determine floor number for layer %s, assumed to be ground-level.";

  @Override
  public String getMessage() {
    return String.format(FMT, entity.getId(), layer);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, entity.url(), entity.getId(), layer);
  }
}
