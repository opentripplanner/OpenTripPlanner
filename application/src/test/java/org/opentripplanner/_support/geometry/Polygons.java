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

  public static final Polygon OSLO = FAC.createPolygon(
    new Coordinate[] {
      Coordinates.of(59.961055202323195, 10.62535658370308),
      Coordinates.of(59.889009435700416, 10.62535658370308),
      Coordinates.of(59.889009435700416, 10.849791142928694),
      Coordinates.of(59.961055202323195, 10.849791142928694),
      Coordinates.of(59.961055202323195, 10.62535658370308),
    }
  );
  public static final Polygon OSLO_FROGNER_PARK = FAC.createPolygon(
    new Coordinate[] {
      Coordinates.of(59.92939032560119, 10.69770054003061),
      Coordinates.of(59.929138466684975, 10.695210909925208),
      Coordinates.of(59.92745319808358, 10.692696865071184),
      Coordinates.of(59.92709930323093, 10.693774304996225),
      Coordinates.of(59.92745914988427, 10.69495957972947),
      Coordinates.of(59.92736919590291, 10.697664535925895),
      Coordinates.of(59.924837887427856, 10.697927604125255),
      Coordinates.of(59.924447953413335, 10.697448767354985),
      Coordinates.of(59.92378800804022, 10.697819761729818),
      Coordinates.of(59.92329018587293, 10.699196446969069),
      Coordinates.of(59.92347619027632, 10.700285749621997),
      Coordinates.of(59.92272030268688, 10.704714696822037),
      Coordinates.of(59.92597766029715, 10.71001707489603),
      Coordinates.of(59.92676341291536, 10.707838597058043),
      Coordinates.of(59.92790300889098, 10.708389137506913),
      Coordinates.of(59.928376832499424, 10.707060536853078),
      Coordinates.of(59.92831087551576, 10.705803789539402),
      Coordinates.of(59.92953431964068, 10.706641515204467),
      Coordinates.of(59.93046383654274, 10.70484606360543),
      Coordinates.of(59.93008590667682, 10.701817874860211),
      Coordinates.of(59.93028982601595, 10.700525251174469),
      Coordinates.of(59.92939032560119, 10.69770054003061),
    }
  );

  public static final Polygon SELF_INTERSECTING = FAC.createPolygon(
    new Coordinate[] {
      Coordinates.of(1, 1),
      Coordinates.of(1, 2),
      Coordinates.of(1, 3),
      Coordinates.of(1, 1),
    }
  );

  public static org.geojson.Polygon toGeoJson(Polygon polygon) {
    var ret = new org.geojson.Polygon();

    var coordinates = Arrays.stream(polygon.getCoordinates())
      .map(c -> new LngLatAlt(c.x, c.y))
      .toList();
    ret.add(coordinates);

    return ret;
  }
}
