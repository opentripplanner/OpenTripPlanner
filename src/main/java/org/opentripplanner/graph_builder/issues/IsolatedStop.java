package org.opentripplanner.graph_builder.issues;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.street.model.vertex.Vertex;

public record IsolatedStop(Vertex vertex) implements DataImportIssue {
  private static final String FMT = "Unable to link stop %s to the street graph";

  @Override
  public String getMessage() {
    return String.format(FMT, vertex.getLabel());
  }

  @Override
  public Vertex getReferencedVertex() {
    return vertex;
  }

  @Override
  public Geometry getGeometry() {
    return GeometryUtils.getGeometryFactory().createPoint(vertex.getCoordinate());
  }
}
