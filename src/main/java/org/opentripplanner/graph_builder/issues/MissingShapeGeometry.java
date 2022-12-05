package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class MissingShapeGeometry implements DataImportIssue {

  public static final String FMT = "Trip %s refers to unknown shape geometry %s";

  final FeedScopedId shapeId;
  final FeedScopedId tripId;

  public MissingShapeGeometry(FeedScopedId tripId, FeedScopedId shapeId) {
    this.tripId = tripId;
    this.shapeId = shapeId;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, tripId, shapeId);
  }
}
