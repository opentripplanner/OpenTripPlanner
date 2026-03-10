package org.opentripplanner.street.geometry;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;

public class Polygons {

  private static final GeometryFactory FAC = GeometryUtils.getGeometryFactory();

  public static final Polygon OSLO = FAC.createPolygon(
    new Coordinate[] {
      new Coordinate(10.62535658370308, 59.961055202323195),
      new Coordinate(10.62535658370308, 59.889009435700416),
      new Coordinate(10.849791142928694, 59.889009435700416),
      new Coordinate(10.849791142928694, 59.961055202323195),
      new Coordinate(10.62535658370308, 59.961055202323195),
    }
  );

  public static final Polygon OSLO_FROGNER_PARK = FAC.createPolygon(
    new Coordinate[] {
      new Coordinate(10.69770054003061, 59.92939032560119),
      new Coordinate(10.695210909925208, 59.929138466684975),
      new Coordinate(10.692696865071184, 59.92745319808358),
      new Coordinate(10.693774304996225, 59.92709930323093),
      new Coordinate(10.69495957972947, 59.92745914988427),
      new Coordinate(10.697664535925895, 59.92736919590291),
      new Coordinate(10.697927604125255, 59.924837887427856),
      new Coordinate(10.697448767354985, 59.924447953413335),
      new Coordinate(10.697819761729818, 59.92378800804022),
      new Coordinate(10.699196446969069, 59.92329018587293),
      new Coordinate(10.700285749621997, 59.92347619027632),
      new Coordinate(10.704714696822037, 59.92272030268688),
      new Coordinate(10.71001707489603, 59.92597766029715),
      new Coordinate(10.707838597058043, 59.92676341291536),
      new Coordinate(10.708389137506913, 59.92790300889098),
      new Coordinate(10.707060536853078, 59.928376832499424),
      new Coordinate(10.705803789539402, 59.92831087551576),
      new Coordinate(10.706641515204467, 59.92953431964068),
      new Coordinate(10.70484606360543, 59.93046383654274),
      new Coordinate(10.701817874860211, 59.93008590667682),
      new Coordinate(10.700525251174469, 59.93028982601595),
      new Coordinate(10.69770054003061, 59.92939032560119),
    }
  );
}
