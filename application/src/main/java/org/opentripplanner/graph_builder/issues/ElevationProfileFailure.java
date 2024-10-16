package org.opentripplanner.graph_builder.issues;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.street.model.edge.Edge;

public record ElevationProfileFailure(Edge edge, String reason) implements DataImportIssue {
  private static final String FMT = "Failed to set elevation profile for %s: %s";

  @Override
  public String getMessage() {
    return String.format(FMT, edge, reason);
  }

  @Override
  public Geometry getGeometry() {
    return edge.getGeometry();
  }
}
