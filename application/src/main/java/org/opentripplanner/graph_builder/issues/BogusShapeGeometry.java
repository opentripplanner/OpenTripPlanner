package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;

public record BogusShapeGeometry(FeedScopedId shapeId) implements DataImportIssue {
  private static final String FMT =
    "Shape geometry for shape_id %s does not have two distinct points.";

  @Override
  public String getMessage() {
    return String.format(FMT, shapeId);
  }
}
