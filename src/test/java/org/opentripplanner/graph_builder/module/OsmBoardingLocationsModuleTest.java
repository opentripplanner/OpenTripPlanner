package org.opentripplanner.graph_builder.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.openstreetmap.OpenStreetMapProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.BoardingLocationToStopLink;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertexBuilder;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.test.support.VariableSource;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

/**
 * We test that the platform area at Herrenberg station (https://www.openstreetmap.org/way/27558650)
 * is correctly linked to the stop even though it is not the closest edge to the stop.
 */
class OsmBoardingLocationsModuleTest {

  File file = new File(ConstantsForTests.HERRENBERG_OSM);
  RegularStop platform = TransitModelForTest
    .stop("de:08115:4512:4:101")
    .withCoordinate(48.59328, 8.86128)
    .build();
  RegularStop busStop = TransitModelForTest.stopForTest("de:08115:4512:5:C", 48.59434, 8.86452);
  RegularStop floatingBusStop = TransitModelForTest.stopForTest(
    "floating-bus-stop",
    48.59417,
    8.86464
  );

  static Stream<Arguments> testCases = Stream.of(
    Arguments.of(
      false,
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
    Arguments.of(true, Set.of("osm:node:768590748"))
  );

  @ParameterizedTest(
    name = "add boarding locations and link them to platform edges when skipVisibility={0}"
  )
  @VariableSource("testCases")
  void addAndLinkBoardingLocations(boolean areaVisibility, Set<String> linkedVertices) {
    var deduplicator = new Deduplicator();
    var graph = new Graph(deduplicator);
    var transitModel = new TransitModel(new StopModel(), deduplicator);

    var provider = new OpenStreetMapProvider(file, false);
    var floatingBusVertex = new TransitStopVertexBuilder()
      .withGraph(graph)
      .withStop(floatingBusStop)
      .withModes(Set.of(TransitMode.BUS))
      .build();
    var floatingBoardingLocation = new OsmBoardingLocationVertex(
      graph,
      "floating-bus-stop",
      floatingBusVertex.getX(),
      floatingBusVertex.getY(),
      new NonLocalizedString("bus stop not connected to street network"),
      Set.of(floatingBusVertex.getStop().getId().getId())
    );
    var osmModule = new OpenStreetMapModule(
      List.of(provider),
      Set.of("ref", "ref:IFOPT"),
      graph,
      DataImportIssueStore.NOOP,
      areaVisibility
    );

    osmModule.buildGraph();

    var platformVertex = new TransitStopVertexBuilder()
      .withGraph(graph)
      .withStop(platform)
      .withModes(Set.of(TransitMode.RAIL))
      .build();
    var busVertex = new TransitStopVertexBuilder()
      .withGraph(graph)
      .withStop(busStop)
      .withModes(Set.of(TransitMode.BUS))
      .build();

    transitModel.index();
    graph.index(transitModel.getStopModel());

    assertEquals(0, busVertex.getIncoming().size());
    assertEquals(0, busVertex.getOutgoing().size());

    assertEquals(0, platformVertex.getIncoming().size());
    assertEquals(0, platformVertex.getOutgoing().size());

    new OsmBoardingLocationsModule(graph, transitModel).buildGraph();

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
      .orElseThrow();

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
      .forEach(e -> assertEquals("Platform 101;102", e.getName().toString()));
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
