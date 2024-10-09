package org.opentripplanner.graph_builder.issues;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.street.model.edge.Edge;

public record ElevationFlattened(Edge edge) implements DataImportIssue {
  private static final String FMT = "Edge %s was steeper than Baldwin Street and was flattened.";

  @Override
  public String getMessage() {
    return String.format(FMT, edge);
  }

  @Override
  public Geometry getGeometry() {
    return edge.getGeometry();
  }
}
