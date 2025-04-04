package org.opentripplanner.graph_builder.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.graph_builder.module.osm.OsmModule;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildService;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.BoardingLocationToStopLink;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexFactory;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

class GerlenhofenTest {

  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();

  static Stream<Arguments> cases() {
    return Stream.of(
      Arguments.of(true, Set.of(VertexLabel.osm(768590748)))
    );
  }

  @ParameterizedTest(
    name = "add boarding locations and link them to platform edges when areaVisibility={0}"
  )
  @MethodSource("cases")
  void addAndLinkBoardingLocations(boolean areaVisibility, Set<String> linkedVertices) {
    File file = ResourceLoader.of(GerlenhofenTest.class).file(
      "gerlenhofen.osm.pbf"
    );
    RegularStop platform = testModel
      .stop("de:09775:1781:1:1")
      .withCoordinate(48.351025,10.036302)
      .build();

    var deduplicator = new Deduplicator();
    var graph = new Graph(deduplicator);
    var timetableRepository = new TimetableRepository(new SiteRepository(), deduplicator);
    var factory = new VertexFactory(graph);

    var provider = new DefaultOsmProvider(file, false);
    var osmInfoRepository = new DefaultOsmInfoGraphBuildRepository();
    var vehicleParkingRepository = new DefaultVehicleParkingRepository();
    var osmModule = OsmModule.of(provider, graph, osmInfoRepository, vehicleParkingRepository)
      .withBoardingAreaRefTags(Set.of("ref", "ref:IFOPT"))
      .withAreaVisibility(areaVisibility)
      .build();

    osmModule.buildGraph();

    var platformVertex = factory.transitStop(
      TransitStopVertex.of().withStop(platform).withModes(Set.of(TransitMode.RAIL))
    );

    timetableRepository.index();
    graph.index(timetableRepository.getSiteRepository());

    assertEquals(0, platformVertex.getIncoming().size());
    assertEquals(0, platformVertex.getOutgoing().size());

    var osmService = new DefaultOsmInfoGraphBuildService(osmInfoRepository);
    new OsmBoardingLocationsModule(graph, osmService, timetableRepository).buildGraph();

    var boardingLocations = graph.getVerticesOfType(OsmBoardingLocationVertex.class);
    assertEquals(5, boardingLocations.size()); // 3 nodes connected to the street network, plus one "floating" and one area centroid created by the module

    assertEquals(1, platformVertex.getIncoming().size());
    assertEquals(1, platformVertex.getOutgoing().size());

    var platformCentroids = boardingLocations
      .stream()
      .filter(l -> l.references.contains(platform.getId().getId()))
      .toList();

    assertEquals(1, platformCentroids.size());

    var platformCentroid = platformCentroids.get(0);

    assertConnections(platformCentroid, Set.of(BoardingLocationToStopLink.class, AreaEdge.class));

    assertEquals(
      linkedVertices,
      platformCentroid
        .getOutgoingStreetEdges()
        .stream()
        .map(Edge::getToVertex)
        .map(Vertex::getLabel)
        .collect(Collectors.toSet())
    );

    assertEquals(
      linkedVertices,
      platformCentroid
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
    Stream.of(busBoardingLocation.getIncoming(), busBoardingLocation.getOutgoing()).forEach(edges ->
      assertEquals(expected, edges.stream().map(Edge::getClass).collect(Collectors.toSet()))
    );
  }

}
