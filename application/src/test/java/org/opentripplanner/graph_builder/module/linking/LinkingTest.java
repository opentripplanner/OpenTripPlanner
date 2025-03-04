package org.opentripplanner.graph_builder.module.linking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.graph_builder.module.linking.TestGraph.addExtraStops;
import static org.opentripplanner.graph_builder.module.linking.TestGraph.addRegularStopGrid;
import static org.opentripplanner.graph_builder.module.linking.TestGraph.link;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

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
      StreetVertex v0 = StreetModelForTest.intersectionVertex("zero", x, y);
      StreetVertex v1 = StreetModelForTest.intersectionVertex("one", x + delta, y + delta);
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

  /**
   * Test that all the stops are linked identically to the street network on two builds of similar
   * graphs with additional stops in one.
   * <p>
   * We do this by building the graphs and then comparing the stop tree caches.
   */
  @Test
  public void testStopsLinkedIdentically() {
    // build the graph without the added stops
    TestOtpModel model = buildGraphNoTransit();
    Graph g1 = model.graph();
    TimetableRepository timetableRepository1 = model.timetableRepository();
    addRegularStopGrid(g1);
    link(g1, timetableRepository1);

    TestOtpModel model2 = buildGraphNoTransit();
    Graph g2 = model2.graph();
    TimetableRepository timetableRepository2 = model2.timetableRepository();
    addExtraStops(g2);
    addRegularStopGrid(g2);
    link(g2, timetableRepository2);

    var transitStopVertices = g1.getVerticesOfType(TransitStopVertex.class);
    assertEquals(1350, transitStopVertices.size());

    // compare the linkages
    for (TransitStopVertex ts : transitStopVertices) {
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

  /** Build a graph in Columbus, OH with no transit */
  public static TestOtpModel buildGraphNoTransit() {
    var deduplicator = new Deduplicator();
    var siteRepository = new SiteRepository();
    var graph = new Graph(deduplicator);
    var timetableRepository = new TimetableRepository(siteRepository, deduplicator);

    File file = ResourceLoader.of(LinkingTest.class).file("columbus.osm.pbf");
    var provider = new DefaultOsmProvider(file, false);
    var osmInfoRepository = new DefaultOsmInfoGraphBuildRepository();
    var vehicleParkingRepository = new DefaultVehicleParkingRepository();

    var osmModule = OsmModule.of(
      provider,
      graph,
      osmInfoRepository,
      vehicleParkingRepository
    ).build();

    osmModule.buildGraph();
    return new TestOtpModel(graph, timetableRepository);
  }

  private static List<StreetTransitStopLink> outgoingStls(final TransitStopVertex tsv) {
    return tsv
      .getOutgoing()
      .stream()
      .filter(StreetTransitStopLink.class::isInstance)
      .map(StreetTransitStopLink.class::cast)
      .sorted(Comparator.comparing(e -> e.getGeometry().getLength()))
      .toList();
  }
}
