package org.opentripplanner.graph_builder.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.openstreetmap.OsmProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.BoardingLocationToStopLink;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertexBuilder;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexFactory;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model._data.TransitModelForTest;
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

  private final TransitModelForTest testModel = TransitModelForTest.of();

  File file = ResourceLoader
    .of(OsmBoardingLocationsModuleTest.class)
    .file("herrenberg-minimal.osm.pbf");
  RegularStop platform = testModel
    .stop("de:08115:4512:4:101")
    .withCoordinate(48.59328, 8.86128)
    .build();
  RegularStop busStop = testModel.stop("de:08115:4512:5:C", 48.59434, 8.86452).build();
  RegularStop floatingBusStop = testModel.stop("floating-bus-stop", 48.59417, 8.86464).build();

  static Stream<Arguments> testCases() {
    return Stream.of(
      Arguments.of(
        false,
        Stream
          .of(302563833L, 3223067049L, 302563836L, 3223067680L, 302563834L, 768590748L, 302563839L)
          .map(VertexLabel::osm)
          .collect(Collectors.toSet())
      ),
      Arguments.of(true, Set.of(VertexLabel.osm(3223067049L), VertexLabel.osm(768590748)))
    );
  }

  @ParameterizedTest(
    name = "add boarding locations and link them to platform edges when skipVisibility={0}"
  )
  @MethodSource("testCases")
  void addAndLinkBoardingLocations(boolean areaVisibility, Set<String> linkedVertices) {
    var deduplicator = new Deduplicator();
    var graph = new Graph(deduplicator);
    var transitModel = new TransitModel(new StopModel(), deduplicator);
    var factory = new VertexFactory(graph);

    var provider = new OsmProvider(file, false);
    var floatingBusVertex = factory.transitStop(
      new TransitStopVertexBuilder().withStop(floatingBusStop).withModes(Set.of(TransitMode.BUS))
    );
    var floatingBoardingLocation = factory.osmBoardingLocation(
      floatingBusVertex.getCoordinate(),
      "floating-bus-stop",
      Set.of(floatingBusVertex.getStop().getId().getId()),
      new NonLocalizedString("bus stop not connected to street network")
    );
    var osmModule = OsmModule
      .of(provider, graph)
      .withBoardingAreaRefTags(Set.of("ref", "ref:IFOPT"))
      .withAreaVisibility(areaVisibility)
      .build();

    osmModule.buildGraph();

    var platformVertex = factory.transitStop(
      new TransitStopVertexBuilder().withStop(platform).withModes(Set.of(TransitMode.RAIL))
    );
    var busVertex = factory.transitStop(
      new TransitStopVertexBuilder().withStop(busStop).withModes(Set.of(TransitMode.BUS))
    );

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
