package org.opentripplanner.graph_builder.module.linking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.graph_builder.module.FakeGraph.addExtraStops;
import static org.opentripplanner.graph_builder.module.FakeGraph.addRegularStopGrid;
import static org.opentripplanner.graph_builder.module.FakeGraph.buildGraphNoTransit;
import static org.opentripplanner.graph_builder.module.FakeGraph.link;

import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.service.TransitModel;

public class LinkingTest {

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
      StreetVertex v0 = new IntersectionVertex(null, "zero", x, y);
      StreetVertex v1 = new IntersectionVertex(null, "one", x + delta, y + delta);
      LineString geom = gf.createLineString(
        new Coordinate[] { v0.getCoordinate(), v1.getCoordinate() }
      );
      double dist = SphericalDistanceLibrary.distance(v0.getCoordinate(), v1.getCoordinate());
      StreetEdge s0 = new StreetEdge(
        v0,
        v1,
        geom,
        "test",
        dist,
        StreetTraversalPermission.ALL,
        false
      );
      StreetEdge s1 = new StreetEdge(
        v1,
        v0,
        (LineString) geom.reverse(),
        "back",
        dist,
        StreetTraversalPermission.ALL,
        true
      );

      // split it but not too close to the end
      double splitVal = Math.random() * 0.95 + 0.025;

      SplitterVertex sv0 = new SplitterVertex(
        null,
        "split",
        x + delta * splitVal,
        y + delta * splitVal,
        new NonLocalizedString("split")
      );
      SplitterVertex sv1 = new SplitterVertex(
        null,
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

  /**
   * Test that all the stops are linked identically to the street network on two builds of similar
   * graphs with additional stops in one.
   * <p>
   * We do this by building the graphs and then comparing the stop tree caches.
   */
  @Test
  public void testStopsLinkedIdentically() throws URISyntaxException {
    // build the graph without the added stops
    TestOtpModel model = buildGraphNoTransit();
    Graph g1 = model.graph();
    TransitModel transitModel1 = model.transitModel();
    addRegularStopGrid(g1, transitModel1);
    link(g1, transitModel1);

    TestOtpModel model2 = buildGraphNoTransit();
    Graph g2 = model2.graph();
    TransitModel transitModel2 = model2.transitModel();
    addExtraStops(g2, transitModel2);
    addRegularStopGrid(g2, transitModel2);
    link(g2, transitModel2);

    // compare the linkages
    for (TransitStopVertex ts : g1.getVerticesOfType(TransitStopVertex.class)) {
      List<StreetTransitStopLink> stls1 = outgoingStls(ts);
      assertTrue(stls1.size() >= 1);

      TransitStopVertex other = (TransitStopVertex) g2.getVertex(ts.getLabel());
      List<StreetTransitStopLink> stls2 = outgoingStls(other);

      assertEquals(stls1.size(), stls2.size(), "Unequal number of links from stop " + ts);

      for (int i = 0; i < stls1.size(); i++) {
        Vertex v1 = stls1.get(i).getToVertex();
        Vertex v2 = stls2.get(i).getToVertex();
        assertEquals(v1.getLat(), v2.getLat(), 1e-10);
        assertEquals(v1.getLon(), v2.getLon(), 1e-10);
      }
    }
  }

  private static List<StreetTransitStopLink> outgoingStls(final TransitStopVertex tsv) {
    return tsv
      .getOutgoing()
      .stream()
      .filter(StreetTransitStopLink.class::isInstance)
      .map(StreetTransitStopLink.class::cast)
      .sorted(Comparator.comparing(e -> e.getGeometry().getLength()))
      .collect(Collectors.toList());
  }
}
