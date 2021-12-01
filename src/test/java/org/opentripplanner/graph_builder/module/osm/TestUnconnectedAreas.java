package org.opentripplanner.graph_builder.module.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.openstreetmap.BinaryOpenStreetMapProvider;
import org.opentripplanner.routing.edgetype.StreetVehicleParkingLink;
import org.opentripplanner.routing.edgetype.VehicleParkingEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.VehicleParkingEntranceVertex;

public class TestUnconnectedAreas {

    /**
     * The P+R.osm.gz file contains 2 park and ride, one a single way area and the other a
     * multipolygon with a hole. Both are not linked to any street, apart from three roads that
     * crosses the P+R with w/o common nodes.
     * 
     * This test just make sure we correctly link those P+R with the street network by creating
     * virtual nodes at the place where the street intersects the P+R areas. See ticket #1562.
     */
    @Test
    public void testUnconnectedCarParkAndRide() {
      DataImportIssueStore issueStore = new DataImportIssueStore(true);
      Graph gg = buildOSMGraph("P+R.osm.pbf", issueStore);

      assertEquals(2, issueStore.getIssues().size());

      var vehicleParkingVertices = gg.getVerticesOfType(VehicleParkingEntranceVertex.class);
      int nParkAndRide = vehicleParkingVertices.size();
      int nParkAndRideLink = gg.getEdgesOfType(StreetVehicleParkingLink.class).size();
      int nParkAndRideEdge = gg.getEdgesOfType(VehicleParkingEdge.class).size();

      assertEquals(11, nParkAndRide);
      assertEquals(30, nParkAndRideLink);
      assertEquals(41, nParkAndRideEdge);
    }

    @Test
    public void testUnconnectedBikeParkAndRide() {
        DataImportIssueStore issueStore = new DataImportIssueStore(true);
        Graph gg = buildOSMGraph("B+R.osm.pbf", issueStore);

        assertEquals(3, issueStore.getIssues().size());

        var vehicleParkingVertices = gg.getVerticesOfType(VehicleParkingEntranceVertex.class);
        int nParkAndRideEntrances = vehicleParkingVertices.size();
        int nParkAndRideLink = gg.getEdgesOfType(StreetVehicleParkingLink.class).size();
        int nParkAndRideEdge = gg.getEdgesOfType(VehicleParkingEdge.class).size();

        assertEquals(11, nParkAndRideEntrances);
        assertEquals(24, nParkAndRideLink);
        assertEquals(31, nParkAndRideEdge);
    }

    /**
     * This test ensures that if a Park and Ride has a node that is exactly atop a node on a way, the graph
     * builder will not loop forever trying to split it. The hackett-pr.osm.gz file contains a park-and-ride lot in
     * Hackettstown, NJ, which demonstrates this behavior. See discussion in ticket 1605.
     */
    @Test
    public void testCoincidentNodeUnconnectedParkAndRide () {
      List<String> connections = testGeometricGraphWithClasspathFile("hackett_pr.osm.pbf", 4, 8);

      assertTrue(connections.contains("osm:node:3096570222"));
      assertTrue(connections.contains("osm:node:3094264704"));
      assertTrue(connections.contains("osm:node:3094264709"));
      assertTrue(connections.contains("osm:node:3096570227"));
    }
    
    /**
     * Test the situation where a road passes over a node of a park and ride but does not have a node there.
     */
     @Test
     public void testRoadPassingOverNode () {
    	 List<String> connections = testGeometricGraphWithClasspathFile("coincident_pr.osm.pbf", 1, 2);
    	 assertTrue(connections.contains("osm:node:-102236"));
     }
     
     /**
      * Test the situation where a park and ride passes over the node of a road but does not have a node there.
      * Additionally, the node of the road is duplicated to test this corner case.
      */
     @Test
     public void testAreaPassingOverNode () {
    	 List<String> connections = testGeometricGraphWithClasspathFile("coincident_pr_reverse.osm.pbf", 1, 2);
    	 assertTrue(connections.contains("osm:node:-102296"));
     }

     /**
     * Test the situation where a road passes over a node of a park and ride but does not have a node there.
      * Additionally, the node of the ring is duplicated to test this corner case.
      */
     @Test
     public void testRoadPassingOverDuplicatedNode () throws URISyntaxException {
         List<String> connections = testGeometricGraphWithClasspathFile("coincident_pr_dupl.osm.pbf", 1, 2);

    	 // depending on what order everything comes out of the spatial index, we will inject one of
    	 // the duplicated nodes into the way. When we get to the other ringsegments, we will just inject
    	 // the node that has already been injected into the way. So either of these cases are valid.
    	 assertTrue(connections.contains("osm:node:-102266") || connections.contains("osm:node:-102267"));
     }

    /**
     * Test the situation where a road passes over an edge of the park and ride. Both ends of the
     * way are connected to the park and ride.
     */
    @Test
    public void testRoadPassingOverParkRide() {
        List<String> connections = testGeometricGraphWithClasspathFile("coincident_pr_overlap.osm.pbf", 2, 4);

        assertTrue(connections.contains("osm:node:-102283"));
        assertTrue(connections.contains("osm:node:-102284"));
    }

    private Graph buildOSMGraph(String osmFileName) {
        return buildOSMGraph(osmFileName, new DataImportIssueStore(false));
      }

      private Graph buildOSMGraph(String osmFileName, DataImportIssueStore issueStore) {
        Graph graph = new Graph();

        OpenStreetMapModule loader = new OpenStreetMapModule();
        loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
        loader.staticParkAndRide = true;
        loader.staticBikeParkAndRide = true;

        var fileUrl = getClass().getResource(osmFileName);
        assertNotNull(fileUrl);
        File file = new File(fileUrl.getFile());

        BinaryOpenStreetMapProvider provider = new BinaryOpenStreetMapProvider(file, false);
        loader.setProvider(provider);
        loader.buildGraph(graph, new HashMap<>(), issueStore);

        StreetLinkerModule streetLinkerModule = new StreetLinkerModule();
        streetLinkerModule.buildGraph(graph, new HashMap<>(), issueStore);

        return graph;
      }
     
     /**
      * We've written several OSM files that exhibit different situations but should show the same results. Test with this code.
      */
     private List<String> testGeometricGraphWithClasspathFile(String fileName, int prCount, int prlCount) {

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
}
