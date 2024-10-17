package org.opentripplanner.graph_builder.issues;

import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;

public record StopNotLinkedForTransfers(TransitStopVertex stop) implements DataImportIssue {
  private static final String FMT = "Stop %s not near any other stops; no transfers are possible.";

  private static final String HTMLFMT =
    "Stop <a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s&layers=T\">\"%s (%s)\"</a> not near any other stops; no transfers are possible.";

  @Override
  public String getMessage() {
    return String.format(FMT, stop);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(
      HTMLFMT,
      stop.getLat(),
      stop.getLon(),
      stop.getDefaultName(),
      stop.getStop().getId()
    );
  }

  @Override
  public Vertex getReferencedVertex() {
    return this.stop;
  }

  @Override
  public Geometry getGeometry() {
    return stop.getStop().getGeometry();
  }
}
