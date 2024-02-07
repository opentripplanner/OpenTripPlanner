package org.opentripplanner._support.geometry;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import org.geojson.GeoJsonObject;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
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
