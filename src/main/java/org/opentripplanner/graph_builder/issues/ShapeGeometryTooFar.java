package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class ShapeGeometryTooFar implements DataImportIssue {

  public static final String FMT =
    "Trip %s is too far from shape geometry %s, using straight line path instead";

  final FeedScopedId shapeId;
  final FeedScopedId tripId;

  public ShapeGeometryTooFar(FeedScopedId tripId, FeedScopedId shapeId) {
    this.tripId = tripId;
    this.shapeId = shapeId;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, tripId, shapeId);
  }
}
