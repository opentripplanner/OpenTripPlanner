package org.opentripplanner.graph_builder.issue.api;

import org.locationtech.jts.geom.Coordinate;

public class OsmUrlGenerator {

  public static String fromCoordinate(Coordinate c) {
    return fromCoordinate(c, 19);
  }

  public static String fromCoordinate(Coordinate c, int zoom) {
    var lat = c.y;
    var lon = c.x;
    return "https://www.openstreetmap.org/?mlat=%s&mlon=%s#map=%s/%s/%s".formatted(
        lat,
        lon,
        zoom,
        lat,
        lon
      );
  }
}
