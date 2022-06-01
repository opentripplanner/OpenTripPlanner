package org.opentripplanner.graph_builder.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.model.Stop;
import org.opentripplanner.openstreetmap.OpenStreetMapProvider;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.BoardingLocationToStopLink;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.OsmBoardingLocationVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.transit.model.network.TransitMode;

/**
 * We test that the platform area at Herrenberg station (https://www.openstreetmap.org/way/27558650)
 * is correctly linked to the stop even though it is not the closest edge to the stop.
 */
class OsmBoardingLocationsModuleTest {

  File file = new File(ConstantsForTests.HERRENBERG_OSM);
  Stop stop = Stop.stopForTest("de:08115:4512:4:101", 48.59328, 8.86128);

  @ParameterizedTest(
    name = "Should add boarding locations and link them to platform edges when skipVisibility={0}"
  )
  @ValueSource(booleans = { true, false })
  void addAndLinkBoardingLocations(boolean skipVisibility) {
    var graph = new Graph();
    var extra = new HashMap<Class<?>, Object>();

    var provider = new OpenStreetMapProvider(file, false);

    var osmModule = new OpenStreetMapModule(List.of(provider), Set.of("ref", "ref:IFOPT"));
    osmModule.skipVisibility = skipVisibility;

    osmModule.buildGraph(graph, extra);

    var stopVertex = new TransitStopVertex(graph, stop, Set.of(TransitMode.RAIL));

    assertEquals(0, stopVertex.getIncoming().size());
    assertEquals(0, stopVertex.getOutgoing().size());

    var boardingLocationsModule = new OsmBoardingLocationsModule();
    boardingLocationsModule.buildGraph(graph, extra);

    var boardingLocations = graph.getVerticesOfType(OsmBoardingLocationVertex.class);
    assertEquals(4, boardingLocations.size()); // 3 nodes plus one area centroid created by the module

    assertEquals(1, stopVertex.getIncoming().size());
    assertEquals(1, stopVertex.getOutgoing().size());

    var platformCentroids = boardingLocations
      .stream()
      .filter(l -> l.references.contains(stop.getId().getId()))
      .toList();

    assertEquals(1, platformCentroids.size());

    var platform = platformCentroids.get(0);

    Stream
      .of(platform.getIncoming(), platform.getOutgoing())
      .forEach(edges ->
        assertEquals(
          Set.of(BoardingLocationToStopLink.class, AreaEdge.class),
          edges.stream().map(Edge::getClass).collect(Collectors.toSet())
        )
      );

    /*
    assertEquals(
      List.of("osm:node:768590748"),
      platform
        .getOutgoingStreetEdges()
        .stream()
        .map(Edge::getToVertex)
        .map(Vertex::getLabel)
        .toList()
    );

    assertEquals(
      List.of("osm:node:768590748"),
      platform
        .getIncomingStreetEdges()
        .stream()
        .map(Edge::getFromVertex)
        .map(Vertex::getLabel)
        .toList()
    );
     */

    platformCentroids
      .stream()
      .flatMap(c -> Stream.concat(c.getIncoming().stream(), c.getOutgoing().stream()))
      .forEach(e -> assertNotNull(e.getName(), "Edge " + e + " returns null for getName()"));

    platformCentroids
      .stream()
      .flatMap(c -> Stream.concat(c.getIncoming().stream(), c.getOutgoing().stream()))
      .filter(StreetEdge.class::isInstance)
      .forEach(e -> assertEquals("101;102", e.getName().toString()));
  }
}
