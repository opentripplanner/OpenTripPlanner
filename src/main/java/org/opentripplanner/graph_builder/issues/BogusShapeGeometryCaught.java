package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class BogusShapeGeometryCaught implements DataImportIssue {

  public static final String FMT =
    "Shape_dist_traveled for shape_id %s is in conflict with stop " +
    "times %s and %s; shape_dist_traveled will not be used";

  final FeedScopedId shapeId;
  final StopTime stA;
  final StopTime stB;

  public BogusShapeGeometryCaught(FeedScopedId shapeId, StopTime stA, StopTime stB) {
    this.shapeId = shapeId;
    this.stA = stA;
    this.stB = stB;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, shapeId, stA, stB);
  }
}
