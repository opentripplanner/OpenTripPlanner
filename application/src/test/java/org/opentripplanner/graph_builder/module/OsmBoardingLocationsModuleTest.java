package org.opentripplanner.graph_builder.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
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

class OsmBoardingLocationsModuleTest {

  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();

  static Stream<Arguments> herrenbergTestCases() {
    return Stream.of(
      Arguments.of(
        false,
        Stream
          .of(302563833L, 3223067049L, 302563836L, 3223067680L, 302563834L, 768590748L, 302563839L)
          .map(VertexLabel::osm)
          .collect(Collectors.toSet())
      ),
      Arguments.of(true, Set.of(VertexLabel.osm(768590748)))
    );
  }

  /**
   * We test that the platform area at Herrenberg station (https://www.openstreetmap.org/way/27558650)
   * is correctly linked to the stop even though it is not the closest edge to the stop.
   */
  @ParameterizedTest(
    name = "add boarding locations and link them to platform edges when skipVisibility={0}"
  )
  @MethodSource("herrenbergTestCases")
  void addAndLinkBoardingLocations(boolean areaVisibility, Set<String> linkedVertices) {
    File file = ResourceLoader
      .of(OsmBoardingLocationsModuleTest.class)
      .file("herrenberg-minimal.osm.pbf");
    RegularStop platform = testModel
      .stop("de:08115:4512:4:101")
      .withCoordinate(48.59328, 8.86128)
      .build();
    RegularStop busStop = testModel.stop("de:08115:4512:5:C", 48.59434, 8.86452).build();
    RegularStop floatingBusStop = testModel.stop("floating-bus-stop", 48.59417, 8.86464).build();

    var deduplicator = new Deduplicator();
    var graph = new Graph(deduplicator);
    var timetableRepository = new TimetableRepository(new SiteRepository(), deduplicator);
    var factory = new VertexFactory(graph);

    var provider = new DefaultOsmProvider(file, false);
    var floatingBusVertex = factory.transitStop(
      TransitStopVertex.of().withStop(floatingBusStop).withModes(Set.of(TransitMode.BUS))
    );
    var floatingBoardingLocation = factory.osmBoardingLocation(
      floatingBusVertex.getCoordinate(),
      "floating-bus-stop",
      Set.of(floatingBusVertex.getStop().getId().getId()),
      new NonLocalizedString("bus stop not connected to street network")
    );
    var osmInfoRepository = new DefaultOsmInfoGraphBuildRepository();
    var vehicleParkingRepository = new DefaultVehicleParkingRepository();
    var osmModule = OsmModule
      .of(provider, graph, osmInfoRepository, vehicleParkingRepository)
      .withBoardingAreaRefTags(Set.of("ref", "ref:IFOPT"))
      .withAreaVisibility(areaVisibility)
      .build();

    osmModule.buildGraph();

    var platformVertex = factory.transitStop(
      TransitStopVertex.of().withStop(platform).withModes(Set.of(TransitMode.RAIL))
    );
    var busVertex = factory.transitStop(
      TransitStopVertex.of().withStop(busStop).withModes(Set.of(TransitMode.BUS))
    );

    timetableRepository.index();
    graph.index(timetableRepository.getSiteRepository());

    assertEquals(0, busVertex.getIncoming().size());
    assertEquals(0, busVertex.getOutgoing().size());

    assertEquals(0, platformVertex.getIncoming().size());
    assertEquals(0, platformVertex.getOutgoing().size());

    var osmService = new DefaultOsmInfoGraphBuildService(osmInfoRepository);
    new OsmBoardingLocationsModule(graph, osmService, timetableRepository).buildGraph();

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

  /**
   * We test that the underground platforms at Moorgate station (https://www.openstreetmap.org/way/1328222021)
   * is correctly linked to the stop even though it is not the closest edge to the stop.
   */
  @Test
  void testLinearPlatforms() {
    var deduplicator = new Deduplicator();
    var graph = new Graph(deduplicator);
    var osmInfoRepository = new DefaultOsmInfoGraphBuildRepository();
    var osmModule = OsmModule
      .of(
        new DefaultOsmProvider(
          ResourceLoader.of(OsmBoardingLocationsModuleTest.class).file("moorgate.osm.pbf"),
          false
        ),
        graph,
        osmInfoRepository,
        new DefaultVehicleParkingRepository()
      )
      .withBoardingAreaRefTags(Set.of("naptan:AtcoCode"))
      .build();
    osmModule.buildGraph();

    var factory = new VertexFactory(graph);

    class TestCase {

      /**
       * The linear platform to be tested
       */
      public final RegularStop platform;

      /**
       * The label of a vertex where the centroid should be connected to
       */
      public final VertexLabel beginLabel;

      /**
       * The label of the other vertex where the centroid should be connected to
       */
      public final VertexLabel endLabel;

      private TransitStopVertex platformVertex = null;

      public TestCase(RegularStop platform, VertexLabel beginLabel, VertexLabel endLabel) {
        this.platform = platform;
        this.beginLabel = beginLabel;
        this.endLabel = endLabel;
      }

      /**
       * Get a TransitStopVertex for the platform in the graph. It is made and added to the graph
       * on the first call.
       */
      TransitStopVertex getPlatformVertex() {
        if (platformVertex == null) {
          platformVertex = factory.transitStop(TransitStopVertex.of().withStop(platform));
        }
        return platformVertex;
      }
    }

    var testCases = List.of(
      new TestCase(
        testModel
          .stop("9100MRGT9")
          .withName(I18NString.of("Moorgate (Platform 9)"))
          .withCoordinate(51.51922107872304, -0.08767468698832413)
          .withPlatformCode("9")
          .build(),
        VertexLabel.osm(12288669589L),
        VertexLabel.osm(12288675219L)
      ),
      new TestCase(
        testModel
          .stop("9400ZZLUMGT3")
          .withName(I18NString.of("Moorgate (Platform 7)"))
          .withCoordinate(51.51919235051611, -0.08769925990953176)
          .withPlatformCode("7")
          .build(),
        VertexLabel.osm(12288669575L),
        VertexLabel.osm(12288675230L)
      )
    );

    for (var testCase : testCases) {
      // test that the platforms are not connected
      var platformVertex = testCase.getPlatformVertex();
      assertEquals(0, platformVertex.getIncoming().size());
      assertEquals(0, platformVertex.getOutgoing().size());

      // test that the vertices to be connected by the centroid are currently connected
      var fromVertex = Objects.requireNonNull(graph.getVertex(testCase.beginLabel));
      var toVertex = Objects.requireNonNull(graph.getVertex(testCase.endLabel));
      assertTrue(
        getEdge(fromVertex, toVertex).isPresent(),
        "malformed test: the vertices where the centroid is supposed to be located between aren't connected"
      );
      assertTrue(
        getEdge(toVertex, fromVertex).isPresent(),
        "malformed test: the vertices where the centroid is supposed to be located between aren't connected"
      );
    }

    var timetableRepository = new TimetableRepository(new SiteRepository(), deduplicator);
    new OsmBoardingLocationsModule(
      graph,
      new DefaultOsmInfoGraphBuildService(osmInfoRepository),
      timetableRepository
    )
      .buildGraph();

    var boardingLocations = graph.getVerticesOfType(OsmBoardingLocationVertex.class);

    for (var testCase : testCases) {
      var platformVertex = testCase.getPlatformVertex();
      var fromVertex = Objects.requireNonNull(graph.getVertex(testCase.beginLabel));
      var toVertex = Objects.requireNonNull(graph.getVertex(testCase.endLabel));

      var centroid = boardingLocations
        .stream()
        .filter(b -> b.references.contains(testCase.platform.getId().getId()))
        .findFirst()
        .orElseThrow();

      // TODO: we should ideally place the centroid vertex directly on the platform by splitting
      // the platform edge, but it is too difficult to touch the splitter code to use a given
      // centroid vertex instead of a generated split vertex, so what we actually do is to directly
      // connect the platform vertex to the split vertex

      // the actual centroid isn't used
      assertEquals(0, centroid.getDegreeIn());
      assertEquals(0, centroid.getDegreeOut());

      for (var vertex : platformVertex.getIncoming()) {
        assertSplitVertex(vertex.getFromVertex(), centroid, fromVertex, toVertex);
      }

      for (var vertex : platformVertex.getOutgoing()) {
        assertSplitVertex(vertex.getToVertex(), centroid, fromVertex, toVertex);
      }
    }
  }

  /**
   * Assert that a split vertex is near to the given centroid, and it is possible to travel between
   * the original vertices through the split vertex in a straight line
   */
  private static void assertSplitVertex(
    Vertex splitVertex,
    OsmBoardingLocationVertex centroid,
    Vertex begin,
    Vertex end
  ) {
    var distance = SphericalDistanceLibrary.distance(
      splitVertex.getCoordinate(),
      centroid.getCoordinate()
    );
    // FIXME: I am not sure why the calculated centroid from the original OSM geometry is about 2 m
    // from the platform
    assertTrue(distance < 4, "The split vertex is more than 4 m apart from the centroid");
    assertConnections(splitVertex, begin, end);

    if (splitVertex != begin && splitVertex != end) {
      var forwardEdges = getEdge(begin, splitVertex)
        .flatMap(first -> getEdge(splitVertex, end).map(second -> List.of(first, second)));
      var backwardEdges = getEdge(end, splitVertex)
        .flatMap(first -> getEdge(splitVertex, begin).map(second -> List.of(first, second)));
      for (var edgeList : List.of(forwardEdges, backwardEdges)) {
        edgeList.ifPresent(edges ->
          assertEquals(
            edges.getFirst().getOutAngle(),
            edges.getLast().getInAngle(),
            "The split vertex is not on a straight line between the connected vertices"
          )
        );
      }
    }
  }

  /**
   * Assert that there is a one-way path from the beginning through the given vertex to the end
   * or vice versa.
   */
  private static void assertConnections(Vertex vertex, Vertex beginning, Vertex end) {
    if (vertex == beginning || vertex == end) {
      assertTrue(beginning.isConnected(end));
    }

    assertTrue(
      (getEdge(beginning, vertex).isPresent() && getEdge(vertex, end).isPresent()) ||
      (getEdge(end, vertex).isPresent() && getEdge(vertex, beginning).isPresent())
    );
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

  private static Optional<StreetEdge> getEdge(Vertex from, Vertex to) {
    return from
      .getOutgoingStreetEdges()
      .stream()
      .filter(edge -> edge.getToVertex() == to)
      .findFirst();
  }
}
