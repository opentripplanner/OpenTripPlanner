package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.graph_builder.issues.ParkAndRideUnlinked;
import org.opentripplanner.graph_builder.module.TestStreetLinkerModule;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.street.model.edge.StreetVehicleParkingLink;
import org.opentripplanner.street.model.edge.VehicleParkingEdge;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

public class UnconnectedAreasTest {

  private static final ResourceLoader RESOURCE_LOADER = ResourceLoader.of(
    UnconnectedAreasTest.class
  );

  /**
   * The P+R.osm.pbf file contains 2 park and ride, one a single way area and the other a
   * multipolygon with a hole. Both are not linked to any street, apart from three roads that
   * crosses the P+R with w/o common nodes.
   * <p>
   * This test just make sure we correctly link those P+R with the street network by creating
   * virtual nodes at the place where the street intersects the P+R areas. See ticket #1562.
   */
  @Test
  public void unconnectedCarParkAndRide() {
    DefaultDataImportIssueStore issueStore = new DefaultDataImportIssueStore();
    Graph gg = buildOsmGraph("P+R.osm.pbf", issueStore);

    assertEquals(1, getParkAndRideUnlinkedIssueCount(issueStore));

    var vehicleParkingVertices = gg.getVerticesOfType(VehicleParkingEntranceVertex.class);
    int nParkAndRide = vehicleParkingVertices.size();
    int nParkAndRideLink = gg.getEdgesOfType(StreetVehicleParkingLink.class).size();
    int nParkAndRideEdge = gg.getEdgesOfType(VehicleParkingEdge.class).size();

    assertEquals(12, nParkAndRide);
    assertEquals(38, nParkAndRideLink);
    assertEquals(42, nParkAndRideEdge);
  }

  @Test
  public void unconnectedBikeParkAndRide() {
    DefaultDataImportIssueStore issueStore = new DefaultDataImportIssueStore();
    Graph gg = buildOsmGraph("B+R.osm.pbf", issueStore);

    assertEquals(2, getParkAndRideUnlinkedIssueCount(issueStore));

    var vehicleParkingVertices = gg.getVerticesOfType(VehicleParkingEntranceVertex.class);
    int nParkAndRideEntrances = vehicleParkingVertices.size();
    int nParkAndRideLink = gg.getEdgesOfType(StreetVehicleParkingLink.class).size();
    int nParkAndRideEdge = gg.getEdgesOfType(VehicleParkingEdge.class).size();

    assertEquals(13, nParkAndRideEntrances);
    assertEquals(32, nParkAndRideLink);
    assertEquals(33, nParkAndRideEdge);
  }

  /**
   * This test ensures that if a Park and Ride has a node that is exactly atop a node on a way, the
   * graph builder will not loop forever trying to split it. The hackett-pr.osm.gz file contains a
   * park-and-ride lot in Hackettstown, NJ, which demonstrates this behavior. See discussion in
   * ticket 1605.
   */
  @Test
  public void testCoincidentNodeUnconnectedParkAndRide() {
    List<VertexLabel> connections = testGeometricGraphWithClasspathFile("hackett_pr.osm.pbf", 4, 8);

    assertTrue(connections.contains(VertexLabel.osm(3096570222L)));
    assertTrue(connections.contains(VertexLabel.osm(3094264704L)));
    assertTrue(connections.contains(VertexLabel.osm(3094264709L)));
    assertTrue(connections.contains(VertexLabel.osm(3096570227L)));
  }

  /**
   * Test the situation where a road passes over a node of a park and ride but does not have a node
   * there.
   */
  @Test
  public void testRoadPassingOverNode() {
    List<VertexLabel> connections = testGeometricGraphWithClasspathFile(
      "coincident_pr.osm.pbf",
      1,
      2
    );
    assertTrue(connections.contains(VertexLabel.osm(-102236L)));
  }

  /**
   * Test the situation where a park and ride passes over the node of a road but does not have a
   * node there. Additionally, the node of the road is duplicated to test this corner case.
   */
  @Test
  public void testAreaPassingOverNode() {
    List<VertexLabel> connections = testGeometricGraphWithClasspathFile(
      "coincident_pr_reverse.osm.pbf",
      1,
      2
    );
    assertTrue(connections.contains(VertexLabel.osm(-102296L)));
  }

  /**
   * Test the situation where a road passes over a node of a park and ride but does not have a node
   * there. Additionally, the node of the ring is duplicated to test this corner case.
   */
  @Test
  public void testRoadPassingOverDuplicatedNode() {
    List<VertexLabel> connections = testGeometricGraphWithClasspathFile(
      "coincident_pr_dupl.osm.pbf",
      1,
      2
    );

    // depending on what order everything comes out of the spatial index, we will inject one of
    // the duplicated nodes into the way. When we get to the other ringsegments, we will just inject
    // the node that has already been injected into the way. So either of these cases are valid.
    assertTrue(
      connections.contains(VertexLabel.osm(-102266)) ||
      connections.contains(VertexLabel.osm(-102267))
    );
  }

  /**
   * Test the situation where a road passes over an edge of the park and ride. Both ends of the way
   * are connected to the park and ride.
   */
  @Test
  public void testRoadPassingOverParkRide() {
    List<VertexLabel> connections = testGeometricGraphWithClasspathFile(
      "coincident_pr_overlap.osm.pbf",
      2,
      4
    );

    assertTrue(connections.contains(VertexLabel.osm(-102283)));
    assertTrue(connections.contains(VertexLabel.osm(-102284)));
  }

  private Graph buildOsmGraph(String osmFileName) {
    return buildOsmGraph(osmFileName, DataImportIssueStore.NOOP);
  }

  private Graph buildOsmGraph(String osmFileName, DataImportIssueStore issueStore) {
    var deduplicator = new Deduplicator();
    var siteRepository = new SiteRepository();
    var graph = new Graph(deduplicator);
    var timetableRepository = new TimetableRepository(siteRepository, deduplicator);
    DefaultOsmProvider provider = new DefaultOsmProvider(RESOURCE_LOADER.file(osmFileName), false);
    OsmModule loader = OsmModule
      .of(
        provider,
        graph,
        new DefaultOsmInfoGraphBuildRepository(),
        new DefaultVehicleParkingRepository()
      )
      .withIssueStore(issueStore)
      .withAreaVisibility(true)
      .withStaticParkAndRide(true)
      .withStaticBikeParkAndRide(true)
      .build();

    loader.buildGraph();

    TestStreetLinkerModule.link(graph, timetableRepository);

    return graph;
  }

  /**
   * We've written several OSM files that exhibit different situations but should show the same
   * results. Test with this code.
   */
  private List<VertexLabel> testGeometricGraphWithClasspathFile(
    String fileName,
    int prCount,
    int prlCount
  ) {
    Graph graph = buildOsmGraph(fileName);

    var vehicleParkingVertices = graph.getVerticesOfType(VehicleParkingEntranceVertex.class);
    int nParkAndRide = vehicleParkingVertices.size();

    int nParkAndRideLink = graph.getEdgesOfType(StreetVehicleParkingLink.class).size();

    assertEquals(prCount, nParkAndRide);
    assertEquals(prlCount, nParkAndRideLink);

    var outgoingEdges = vehicleParkingVertices
      .stream()
      .flatMap(v -> v.getOutgoing().stream())
      .filter(e -> !(e instanceof VehicleParkingEdge))
      // make sure it is connected
      .peek(e -> assertTrue(e instanceof StreetVehicleParkingLink))
      .map(StreetVehicleParkingLink.class::cast)
      .collect(Collectors.toCollection(HashSet::new));

    List<VertexLabel> connections = outgoingEdges
      .stream()
      .map(e -> e.getToVertex().getLabel())
      .collect(Collectors.toList());

    // Test symmetry
    vehicleParkingVertices
      .stream()
      .flatMap(v -> v.getIncoming().stream())
      .filter(e -> !(e instanceof VehicleParkingEdge))
      .peek(e -> assertTrue(e instanceof StreetVehicleParkingLink))
      .map(StreetVehicleParkingLink.class::cast)
      .forEach(e -> assertTrue(connections.contains(e.getFromVertex().getLabel())));

    return connections;
  }

  private int getParkAndRideUnlinkedIssueCount(DefaultDataImportIssueStore issueStore) {
    return (int) issueStore
      .listIssues()
      .stream()
      .filter(dataImportIssue -> dataImportIssue instanceof ParkAndRideUnlinked)
      .count();
  }
}
