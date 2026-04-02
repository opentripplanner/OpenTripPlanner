package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public record ShapeGeometryTooFar(FeedScopedId tripId, FeedScopedId shapeId) implements
  DataImportIssue {
  private static final String FMT =
    "Trip %s is too far from shape geometry %s, using straight line path instead";

  @Override
  public String getMessage() {
    return String.format(FMT, tripId, shapeId);
  }
}
