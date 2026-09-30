package org.opentripplanner.graph_builder.module.transfer;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.vertex.TransitStopVertex;

class StopNotLinkedForTransfers implements DataImportIssue {

  private static final String FMT = "Stop %s not near any other stops; no transfers are possible.";

  private static final String HTMLFMT =
    "Stop <a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s&layers=T\">\"%s (%s)\"</a> not near any other stops; no transfers are possible.";

  private final double latitude;
  private final double longitude;
  private final String name;
  private final FeedScopedId id;

  StopNotLinkedForTransfers(TransitStopVertex stop) {
    this.latitude = stop.getLat();
    this.longitude = stop.getLon();
    this.name = stop.getDefaultName();
    this.id = stop.getId();
    stop.getCoordinate();
  }

  @Override
  public String getMessage() {
    return String.format(FMT, id);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(HTMLFMT, latitude, longitude, name, id);
  }

  @Override
  public Geometry getGeometry() {
    return GeometryUtils.getGeometryFactory().createPoint(new Coordinate(longitude, latitude));
  }
}
