package org.opentripplanner.ext.traveltime.geometry;

import static org.locationtech.jts.geom.CoordinateArrays.toCoordinateArray;
import static org.locationtech.jts.geom.GeometryFactory.toGeometryArray;
import static org.locationtech.jts.geom.GeometryFactory.toLinearRingArray;
import static org.locationtech.jts.geom.GeometryFactory.toPolygonArray;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.locationtech.jts.algorithm.Area;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compute isoline based on a Delaunay triangulation of z samplings.
 *
 * It will compute an isoline for a given z0 value. The isoline is composed of a list of n polygons,
 * CW for normal polygons, CCW for "holes". The isoline computation can be called multiple times on
 * the same builder for different z0 value: this will reduce the number of Fz sampling as they are
 * cached in the builder, and reduce the number of time the Delaunay triangulation has to be built.
 *
 * The algorithm is rather simple: for each edges of the triangulation check if the edge is
 * "cutting" (ie crossing the z0 plane). Then start for each unprocessed cutting edge using a walk
 * algorithm, keeping high z0 always one the same side, to build a set of closed polygons. Then
 * process each polygon to punch holes: a CW polygon is a hole in a larger CCW polygon, a CCW
 * polygon is an island (shell).
 *
 * @author laurent
 */
public class DelaunayIsolineBuilder<TZ> implements IsolineBuilder<TZ> {

  private static final Logger LOG = LoggerFactory.getLogger(DelaunayIsolineBuilder.class);

  private final ZMetric<TZ> zMetric;

  private final DelaunayTriangulation<TZ> triangulation;

  private final GeometryFactory geometryFactory = new GeometryFactory();

  private final List<Geometry> debugGeom = new ArrayList<>();

  private boolean debug = false;

  /**
   * Create an object to compute isolines. One may call several time computeIsoline on the same
   * object, with different z0 values.
   *
   * @param triangulation The triangulation to process. Must be closed (no edge at the border
   *        should intersect).
   * @param zMetric The Z metric (intersection detection and interpolation method).
   */
  public DelaunayIsolineBuilder(DelaunayTriangulation<TZ> triangulation, ZMetric<TZ> zMetric) {
    this.triangulation = triangulation;
    this.zMetric = zMetric;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  @Override
  public Geometry computeIsoline(TZ z0) {
    Queue<DelaunayEdge<TZ>> processQ = new ArrayDeque<>(triangulation.edgesCount());
    for (DelaunayEdge<TZ> e : triangulation.edges()) {
      e.setProcessed(false);
      processQ.add(e);
    }

    if (debug) generateDebugGeometry(z0);

    List<LinearRing> rings = new ArrayList<>();
    while (!processQ.isEmpty()) {
      DelaunayEdge<TZ> e = processQ.remove();
      if (e.isProcessed()) continue;
      e.setProcessed(true);
      int cut = zMetric.cut(e.getA().getZ(), e.getB().getZ(), z0);
      if (cut == 0) {
        continue; // While, next edge
      }
      List<Coordinate> polyPoints = new ArrayList<>();
      boolean ccw = cut > 0;
      while (true) {
        // Add a point to polyline
        Coordinate cA = e.getA().getCoordinates();
        Coordinate cB = e.getB().getCoordinates();
        double k = zMetric.interpolate(e.getA().getZ(), e.getB().getZ(), z0);
        Coordinate cC = new Coordinate(cA.x * (1.0 - k) + cB.x * k, cA.y * (1.0 - k) + cB.y * k);
        polyPoints.add(cC);
        e.setProcessed(true);
        DelaunayEdge<TZ> E1 = e.getEdge1(ccw);
        DelaunayEdge<TZ> E2 = e.getEdge2(ccw);
        int cut1 = E1 == null ? 0 : zMetric.cut(E1.getA().getZ(), E1.getB().getZ(), z0);
        int cut2 = E2 == null ? 0 : zMetric.cut(E2.getA().getZ(), E2.getB().getZ(), z0);
        boolean ok1 = cut1 != 0 && !E1.isProcessed();
        boolean ok2 = cut2 != 0 && !E2.isProcessed();
        if (ok1) {
          e = E1;
          ccw = cut1 > 0;
        } else if (ok2) {
          e = E2;
          ccw = cut2 > 0;
        } else {
          // This must be the end of the polyline...
          break;
        }
      }
      // Close the polyline
      polyPoints.add(polyPoints.get(0));
      if (polyPoints.size() > 5) {
        // If the ring is smaller than 4 points do not add it,
        // that will remove too small islands or holes.
        LinearRing ring = geometryFactory.createLinearRing(toCoordinateArray(polyPoints));
        rings.add(ring);
      }
    }
    return punchHoles(rings);
  }

  private void generateDebugGeometry(TZ z0) {
    debug = false;
    for (DelaunayEdge<TZ> e : triangulation.edges()) {
      Coordinate cA = e.getA().getCoordinates();
      Coordinate cB = e.getB().getCoordinates();
      debugGeom.add(geometryFactory.createLineString(new Coordinate[] { cA, cB }));
      if (zMetric.cut(e.getA().getZ(), e.getB().getZ(), z0) != 0) {
        double k = zMetric.interpolate(e.getA().getZ(), e.getB().getZ(), z0);
        Coordinate cC = new Coordinate(cA.x * (1.0 - k) + cB.x * k, cA.y * (1.0 - k) + cB.y * k);
        debugGeom.add(geometryFactory.createPoint(cC));
      }
    }
  }

  public final Geometry getDebugGeometry() {
    return geometryFactory.createGeometryCollection(toGeometryArray(debugGeom));
  }

  @SuppressWarnings("unchecked")
  private MultiPolygon punchHoles(List<LinearRing> rings) {
    List<Polygon> shells = new ArrayList<>(rings.size());
    List<LinearRing> holes = new ArrayList<>(rings.size() / 2);
    // 1. Split the polygon list in two: shells and holes (CCW and CW)
    for (LinearRing ring : rings) {
      if (Area.ofRingSigned(ring.getCoordinateSequence()) > 0.0) {
        holes.add(ring);
      } else {
        shells.add(geometryFactory.createPolygon(ring));
      }
    }
    // 2. Sort the shells based on number of points to optimize step 3.
    shells.sort((o1, o2) -> o2.getNumPoints() - o1.getNumPoints());
    for (Polygon shell : shells) {
      shell.setUserData(new ArrayList<LinearRing>());
    }
    // 3. For each hole, determine which shell it fits in.
    int nHolesFailed = 0;
    for (LinearRing hole : holes) {
      outer:{
        // Probably most of the time, the first shell will be the one
        for (Polygon shell : shells) {
          if (shell.contains(hole)) {
            ((List<LinearRing>) shell.getUserData()).add(hole);
            break outer;
          }
        }
        // This should not happen, but do not break bad here
        // as loosing a hole is not critical, we still have
        // sensible data to return.
        nHolesFailed += 1;
      }
    }
    if (nHolesFailed > 0) {
      LOG.error("Could not find a shell for {} holes.", nHolesFailed);
    }
    // 4. Build the list of punched polygons
    List<Polygon> punched = new ArrayList<>(shells.size());
    for (Polygon shell : shells) {
      List<LinearRing> shellHoles = ((List<LinearRing>) shell.getUserData());
      punched.add(
        geometryFactory.createPolygon(shell.getExteriorRing(), toLinearRingArray(shellHoles))
      );
    }
    return geometryFactory.createMultiPolygon(toPolygonArray(punched));
  }
}
