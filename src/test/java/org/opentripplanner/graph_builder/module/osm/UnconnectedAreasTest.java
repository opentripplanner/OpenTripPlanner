package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.graph_builder.issues.ParkAndRideUnlinked;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.openstreetmap.OpenStreetMapProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.StreetVehicleParkingLink;
import org.opentripplanner.street.model.edge.VehicleParkingEdge;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

public class UnconnectedAreasTest {

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
    Graph gg = buildOSMGraph("P+R.osm.pbf", issueStore);

    assertEquals(1, getParkAndRideUnlinkedIssueCount(issueStore));

    var vehicleParkingVertices = gg.getVerticesOfType(VehicleParkingEntranceVertex.class);
    int nParkAndRide = vehicleParkingVertices.size();
    int nParkAndRideLink = gg.getEdgesOfType(StreetVehicleParkingLink.class).size();
    int nParkAndRideEdge = gg.getEdgesOfType(VehicleParkingEdge.class).size();

    assertEquals(11, nParkAndRide);
    assertEquals(30, nParkAndRideLink);
    assertEquals(41, nParkAndRideEdge);
  }

  @Test
  public void unconnectedBikeParkAndRide() {
    DefaultDataImportIssueStore issueStore = new DefaultDataImportIssueStore();
    Graph gg = buildOSMGraph("B+R.osm.pbf", issueStore);

    assertEquals(2, getParkAndRideUnlinkedIssueCount(issueStore));

    var vehicleParkingVertices = gg.getVerticesOfType(VehicleParkingEntranceVertex.class);
    int nParkAndRideEntrances = vehicleParkingVertices.size();
    int nParkAndRideLink = gg.getEdgesOfType(StreetVehicleParkingLink.class).size();
    int nParkAndRideEdge = gg.getEdgesOfType(VehicleParkingEdge.class).size();

    assertEquals(11, nParkAndRideEntrances);
    assertEquals(24, nParkAndRideLink);
    assertEquals(31, nParkAndRideEdge);
  }

  /**
   * This test ensures that if a Park and Ride has a node that is exactly atop a node on a way, the
   * graph builder will not loop forever trying to split it. The hackett-pr.osm.gz file contains a
   * park-and-ride lot in Hackettstown, NJ, which demonstrates this behavior. See discussion in
   * ticket 1605.
   */
  @Test
  public void testCoincidentNodeUnconnectedParkAndRide() {
    List<String> connections = testGeometricGraphWithClasspathFile("hackett_pr.osm.pbf", 4, 8);

    assertTrue(connections.contains("osm:node:3096570222"));
    assertTrue(connections.contains("osm:node:3094264704"));
    assertTrue(connections.contains("osm:node:3094264709"));
    assertTrue(connections.contains("osm:node:3096570227"));
  }

  /**
   * Test the situation where a road passes over a node of a park and ride but does not have a node
   * there.
   */
  @Test
  public void testRoadPassingOverNode() {
    List<String> connections = testGeometricGraphWithClasspathFile("coincident_pr.osm.pbf", 1, 2);
    assertTrue(connections.contains("osm:node:-102236"));
  }

  /**
   * Test the situation where a park and ride passes over the node of a road but does not have a
   * node there. Additionally, the node of the road is duplicated to test this corner case.
   */
  @Test
  public void testAreaPassingOverNode() {
    List<String> connections = testGeometricGraphWithClasspathFile(
      "coincident_pr_reverse.osm.pbf",
      1,
      2
    );
    assertTrue(connections.contains("osm:node:-102296"));
  }

  /**
   * Test the situation where a road passes over a node of a park and ride but does not have a node
   * there. Additionally, the node of the ring is duplicated to test this corner case.
   */
  @Test
  public void testRoadPassingOverDuplicatedNode() {
    List<String> connections = testGeometricGraphWithClasspathFile(
      "coincident_pr_dupl.osm.pbf",
      1,
      2
    );

    // depending on what order everything comes out of the spatial index, we will inject one of
    // the duplicated nodes into the way. When we get to the other ringsegments, we will just inject
    // the node that has already been injected into the way. So either of these cases are valid.
    assertTrue(
      connections.contains("osm:node:-102266") || connections.contains("osm:node:-102267")
    );
  }

  /**
   * Test the situation where a road passes over an edge of the park and ride. Both ends of the way
   * are connected to the park and ride.
   */
  @Test
  public void testRoadPassingOverParkRide() {
    List<String> connections = testGeometricGraphWithClasspathFile(
      "coincident_pr_overlap.osm.pbf",
      2,
      4
    );

    assertTrue(connections.contains("osm:node:-102283"));
    assertTrue(connections.contains("osm:node:-102284"));
  }

  private Graph buildOSMGraph(String osmFileName) {
    return buildOSMGraph(osmFileName, DataImportIssueStore.NOOP);
  }

  private Graph buildOSMGraph(String osmFileName, DataImportIssueStore issueStore) {
    var deduplicator = new Deduplicator();
    var stopModel = new StopModel();
    var graph = new Graph(deduplicator);
    var transitModel = new TransitModel(stopModel, deduplicator);
    var fileUrl = getClass().getResource(osmFileName);
    Assertions.assertNotNull(fileUrl);
    File file = new File(fileUrl.getFile());

    OpenStreetMapProvider provider = new OpenStreetMapProvider(file, false);
    OpenStreetMapModule loader = new OpenStreetMapModule(
      List.of(provider),
      Set.of(),
      graph,
      issueStore,
      true
    );
    loader.staticParkAndRide = true;
    loader.staticBikeParkAndRide = true;

    loader.buildGraph();

    StreetLinkerModule.linkStreetsForTestOnly(graph, transitModel);

    return graph;
  }

  /**
   * We've written several OSM files that exhibit different situations but should show the same
   * results. Test with this code.
   */
  private List<String> testGeometricGraphWithClasspathFile(
    String fileName,
    int prCount,
    int prlCount
  ) {
    Graph graph = buildOSMGraph(fileName);

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

    List<String> connections = outgoingEdges
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
