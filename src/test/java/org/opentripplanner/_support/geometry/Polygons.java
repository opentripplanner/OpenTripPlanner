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
        new Coordinate(52.616841, 13.224692810),
        new Coordinate(52.6168419, 13.224692810),
        new Coordinate(52.3915238, 13.646107734),
        new Coordinate(52.616841, 13.646107734),
        new Coordinate(52.616841, 13.224692810),
      }
    );

  public static org.geojson.Polygon toGeoJson(Polygon polygon) {
    var ret = new org.geojson.Polygon();

    var coordinates = Arrays
      .stream(polygon.getCoordinates())
      .map(c -> new LngLatAlt(c.y, c.x))
      .toList();
    ret.add(coordinates);

    return ret;
  }
}
