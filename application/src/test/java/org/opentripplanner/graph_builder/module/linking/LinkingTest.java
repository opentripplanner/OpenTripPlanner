package org.opentripplanner.graph_builder.module.linking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.graph_builder.module.linking.TestGraph.addExtraStops;
import static org.opentripplanner.graph_builder.module.linking.TestGraph.addRegularStopGrid;
import static org.opentripplanner.graph_builder.module.linking.TestGraph.link;

import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

public class LinkingTest {

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
    assertEquals(966, transitStopVertices.size());

    // Count unlinked stops, these are stops that are more than ~150 meters from the road network
    int unlinkedStopsCounter = 0;
    // compare the linkages
    for (TransitStopVertex ts : transitStopVertices) {
      List<StreetTransitStopLink> stls1 = outgoingStls(ts);
      if (stls1.isEmpty()) {
        ++unlinkedStopsCounter;
      }

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
    assertEquals(155, unlinkedStopsCounter);
  }

  /** Build a graph in Columbus, OH with no transit */
  public static TestOtpModel buildGraphNoTransit() {
    var siteRepository = new SiteRepository();
    var graph = new Graph();
    var timetableRepository = new TimetableRepository(siteRepository);
    var file = ResourceLoader.of(LinkingTest.class).file("columbus.osm.pbf");
    var provider = new DefaultOsmProvider(file, false);

    OsmModuleTestFactory.of(provider).withGraph(graph).builder().build().buildGraph();

    return new TestOtpModel(
      graph,
      timetableRepository,
      TransferServiceTestFactory.defaultTransferRepository()
    );
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
