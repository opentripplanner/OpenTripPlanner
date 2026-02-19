package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;

public class StreetEdgeSplittingTest {

  /**
   * Ensure that splitting edges yields edges that are identical in length for forward and back
   * edges. StreetEdges have lengths expressed internally in mm, and we want to be sure that not
   * only do they sum to the same values but also that they
   */
  @Test
  public void testSplitting() {
    GeometryFactory gf = GeometryUtils.getGeometryFactory();
    double x = -122.123;
    double y = 37.363;
    for (double delta = 0; delta <= 2; delta += 0.005) {
      StreetVertex v0 = intersectionVertex("zero", x, y);
      StreetVertex v1 = intersectionVertex("one", x + delta, y + delta);
      LineString geom = gf.createLineString(
        new Coordinate[] { v0.getCoordinate(), v1.getCoordinate() }
      );
      double dist = SphericalDistanceLibrary.distance(v0.getCoordinate(), v1.getCoordinate());
      StreetEdge s0 = new StreetEdgeBuilder<>()
        .withFromVertex(v0)
        .withToVertex(v1)
        .withGeometry(geom)
        .withName("test")
        .withMeterLength(dist)
        .withPermission(StreetTraversalPermission.ALL)
        .withBack(false)
        .buildAndConnect();
      StreetEdge s1 = new StreetEdgeBuilder<>()
        .withFromVertex(v1)
        .withToVertex(v0)
        .withGeometry(geom.reverse())
        .withName("back")
        .withMeterLength(dist)
        .withPermission(StreetTraversalPermission.ALL)
        .withBack(true)
        .buildAndConnect();

      // split it but not too close to the end
      double splitVal = Math.random() * 0.95 + 0.025;

      SplitterVertex sv0 = new SplitterVertex(
        "split",
        x + delta * splitVal,
        y + delta * splitVal,
        new NonLocalizedString("split")
      );
      SplitterVertex sv1 = new SplitterVertex(
        "split",
        x + delta * splitVal,
        y + delta * splitVal,
        new NonLocalizedString("split")
      );

      var sp0 = s0.splitDestructively(sv0);
      var sp1 = s1.splitDestructively(sv1);

      // distances expressed internally in mm so this epsilon is plenty good enough to ensure that they
      // have the same values
      assertEquals(sp0.head().getDistanceMeters(), sp1.tail().getDistanceMeters(), 0.0000001);
      assertEquals(sp0.tail().getDistanceMeters(), sp1.head().getDistanceMeters(), 0.0000001);
      assertFalse(sp0.head().isBack());
      assertFalse(sp0.tail().isBack());
      assertTrue(sp1.head().isBack());
      assertTrue(sp1.tail().isBack());
    }
  }
}
