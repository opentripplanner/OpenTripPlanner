package org.opentripplanner.graph_builder.issues;

import java.time.Duration;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.api.OsmUrlGenerator;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.utils.time.DurationUtils;

public record IsolatedStop(TransitStopVertex vertex, Duration maxWalk) implements DataImportIssue {
  private static final String FMT = "Stop %s is isolated, only %s of walking possible";

  @Override
  public String getMessage() {
    return String.format(FMT, vertex.getId(), DurationUtils.durationToStr(maxWalk));
  }

  @Override
  public int getPriority() {
    return (int) maxWalk.toSeconds();
  }

  @Override
  public Vertex getReferencedVertex() {
    return vertex;
  }

  @Override
  public Geometry getGeometry() {
    return GeometryUtils.getGeometryFactory().createPoint(vertex.getCoordinate());
  }

  @Override
  public String getHTMLMessage() {
    return "<a href='%s'>Stop %s</a> is isolated, only %s of walking possible".formatted(
      OsmUrlGenerator.fromCoordinate(vertex.getCoordinate()),
      vertex.getId(),
      DurationUtils.durationToStr(maxWalk)
    );
  }
}
