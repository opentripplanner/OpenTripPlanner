package org.opentripplanner.graph_builder.issues;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.OsmUrlGenerator;
import org.opentripplanner.street.model.edge.Edge;

public record ElevationFlattened(Edge edge) implements DataImportIssue {
  private static final String FMT = "Edge %s was steeper than 100 percent and flattened.";

  @Override
  public String getMessage() {
    return String.format(FMT, edge);
  }

  @Override
  public Geometry getGeometry() {
    return edge.getGeometry();
  }

  @Override
  public String getHTMLMessage() {
    return "<a href='%s'>Edge %s</a> was steeper than 100 percent and flattened.".formatted(
      OsmUrlGenerator.fromCoordinate(edge.getFromVertex().getCoordinate()),
      edge.getName()
    );
  }
}
