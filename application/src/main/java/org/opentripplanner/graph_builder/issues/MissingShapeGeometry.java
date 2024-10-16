package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record MissingShapeGeometry(FeedScopedId tripId, FeedScopedId shapeId)
  implements DataImportIssue {
  private static final String FMT = "Trip %s refers to unknown shape geometry %s";

  @Override
  public String getMessage() {
    return String.format(FMT, tripId, shapeId);
  }
}
