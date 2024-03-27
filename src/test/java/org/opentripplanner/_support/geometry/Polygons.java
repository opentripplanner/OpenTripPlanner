package org.opentripplanner._support.geometry;

import java.util.Arrays;
import org.geojson.LngLatAlt;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.framework.geometry.GeometryUtils;

public class Polygons {

  public static final Polygon BERLIN = GeometryUtils
    .getGeometryFactory()
    .createPolygon(
      new Coordinate[] {
        Coordinates.of(52.616841, 13.224692810),
        Coordinates.of(52.616841, 13.646107734),
        Coordinates.of(52.3915238, 13.646107734),
        Coordinates.of(52.396421, 13.2268067),
        Coordinates.of(52.616841, 13.224692810),
      }
    );

  public static org.geojson.Polygon toGeoJson(Polygon polygon) {
    var ret = new org.geojson.Polygon();

    var coordinates = Arrays
      .stream(polygon.getCoordinates())
      .map(c -> new LngLatAlt(c.x, c.y))
      .toList();
    ret.add(coordinates);

    return ret;
  }
}
