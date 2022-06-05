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
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.model.Stop;
import org.opentripplanner.openstreetmap.OpenStreetMapProvider;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.BoardingLocationToStopLink;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.OsmBoardingLocationVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.test.support.VariableSource;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.util.NonLocalizedString;

/**
 * We test that the platform area at Herrenberg station (https://www.openstreetmap.org/way/27558650)
 * is correctly linked to the stop even though it is not the closest edge to the stop.
 */
class OsmBoardingLocationsModuleTest {

  static Stream<Arguments> testCases = Stream.of(
    Arguments.of(
      true,
      Set.of(
        "osm:node:302563833",
        "osm:node:3223067049",
        "osm:node:302563836",
        "osm:node:3223067680",
        "osm:node:302563834",
        "osm:node:768590748",
        "osm:node:302563839"
      )
    ),
    Arguments.of(false, Set.of("osm:node:768590748"))
  );
  File file = new File(ConstantsForTests.HERRENBERG_OSM);
  Stop platform = Stop.stopForTest("de:08115:4512:4:101", 48.59328, 8.86128);
  Stop busStop = Stop.stopForTest("de:08115:4512:5:C", 48.59434, 8.86452);
  Stop floatingBusStop = Stop.stopForTest("floating-bus-stop", 48.59417, 8.86464);

  @ParameterizedTest(
    name = "add boarding locations and link them to platform edges when skipVisibility={0}"
  )
  @VariableSource("testCases")
  void addAndLinkBoardingLocations(boolean skipVisibility, Set<String> linkedVertices) {
    var graph = new Graph();
    var extra = new HashMap<Class<?>, Object>();

    var provider = new OpenStreetMapProvider(file, false);
    var floatingBusVertex = new TransitStopVertex(graph, floatingBusStop, Set.of(TransitMode.BUS));
    var floatingBoardingLocation = new OsmBoardingLocationVertex(
      graph,
      "floating-bus-stop",
      floatingBusVertex.getX(),
      floatingBusVertex.getY(),
      new NonLocalizedString("bus stop not connected to street network"),
      Set.of(floatingBusVertex.getStop().getId().getId())
    );
    var osmModule = new OpenStreetMapModule(List.of(provider), Set.of("ref", "ref:IFOPT"));
    osmModule.skipVisibility = skipVisibility;

    osmModule.buildGraph(graph, extra);

    var platformVertex = new TransitStopVertex(graph, platform, Set.of(TransitMode.RAIL));
    var busVertex = new TransitStopVertex(graph, busStop, Set.of(TransitMode.BUS));

    assertEquals(0, busVertex.getIncoming().size());
    assertEquals(0, busVertex.getOutgoing().size());

    assertEquals(0, platformVertex.getIncoming().size());
    assertEquals(0, platformVertex.getOutgoing().size());

    var boardingLocationsModule = new OsmBoardingLocationsModule();
    boardingLocationsModule.buildGraph(graph, extra);

    var boardingLocations = graph.getVerticesOfType(OsmBoardingLocationVertex.class);
    assertEquals(5, boardingLocations.size()); // 3 nodes connected to the street network, plus one "floating" and one area centroid created by the module

    assertEquals(1, platformVertex.getIncoming().size());
    assertEquals(1, platformVertex.getOutgoing().size());

    assertEquals(1, busVertex.getIncoming().size());
    assertEquals(1, busVertex.getOutgoing().size());

    var platformCentroids = boardingLocations
      .stream()
      .filter(l -> l.references.contains(platform.getId().getId()))
      .toList();

    var busBoardingLocation = boardingLocations
      .stream()
      .filter(b -> b.references.contains(busStop.getId().getId()))
      .findFirst()
      .get();

    assertConnections(
      busBoardingLocation,
      Set.of(BoardingLocationToStopLink.class, StreetEdge.class)
    );

    assertConnections(
      floatingBoardingLocation,
      Set.of(BoardingLocationToStopLink.class, StreetEdge.class)
    );

    assertEquals(1, platformCentroids.size());

    var platform = platformCentroids.get(0);

    assertConnections(platform, Set.of(BoardingLocationToStopLink.class, AreaEdge.class));

    assertEquals(
      linkedVertices,
      platform
        .getOutgoingStreetEdges()
        .stream()
        .map(Edge::getToVertex)
        .map(Vertex::getLabel)
        .collect(Collectors.toSet())
    );

    assertEquals(
      linkedVertices,
      platform
        .getIncomingStreetEdges()
        .stream()
        .map(Edge::getFromVertex)
        .map(Vertex::getLabel)
        .collect(Collectors.toSet())
    );

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

  private void assertConnections(
    OsmBoardingLocationVertex busBoardingLocation,
    Set<Class<? extends Edge>> expected
  ) {
    Stream
      .of(busBoardingLocation.getIncoming(), busBoardingLocation.getOutgoing())
      .forEach(edges ->
        assertEquals(expected, edges.stream().map(Edge::getClass).collect(Collectors.toSet()))
      );
  }
}
