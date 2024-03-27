package org.opentripplanner._support.geometry;

import java.util.Arrays;
import org.geojson.LngLatAlt;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.framework.geometry.GeometryUtils;

public class Polygons {

  private static final GeometryFactory FAC = GeometryUtils.getGeometryFactory();
  public static final Polygon BERLIN = FAC.createPolygon(
    new Coordinate[] {
      Coordinates.of(52.616841, 13.224692810),
      Coordinates.of(52.616841, 13.646107734),
      Coordinates.of(52.3915238, 13.646107734),
      Coordinates.of(52.396421, 13.2268067),
      Coordinates.of(52.616841, 13.224692810),
    }
  );

  public static Polygon OSLO = FAC.createPolygon(
    new Coordinate[] {
      Coordinates.of(59.961055202323195, 10.62535658370308),
      Coordinates.of(59.889009435700416, 10.62535658370308),
      Coordinates.of(59.889009435700416, 10.849791142928694),
      Coordinates.of(59.961055202323195, 10.849791142928694),
      Coordinates.of(59.961055202323195, 10.62535658370308),
    }
  );
  public static Polygon OSLO_FROGNER_PARK = FAC.createPolygon(
    new Coordinate[] {
      Coordinates.of(59.93112978539807, 10.691099320272173),
      Coordinates.of(59.92231848097069, 10.691099320272173),
      Coordinates.of(59.92231848097069, 10.711758464910503),
      Coordinates.of(59.92231848097069, 10.691099320272173),
      Coordinates.of(59.93112978539807, 10.691099320272173),
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
