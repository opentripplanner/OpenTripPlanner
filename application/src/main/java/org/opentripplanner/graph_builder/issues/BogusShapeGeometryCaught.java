package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record BogusShapeGeometryCaught(FeedScopedId shapeId, StopTime stA, StopTime stB)
  implements DataImportIssue {
  private static final String FMT =
    "Shape_dist_traveled for shape_id %s is in conflict with stop " +
    "times %s and %s; shape_dist_traveled will not be used";

  @Override
  public String getMessage() {
    return String.format(FMT, shapeId, stA, stB);
  }
}
