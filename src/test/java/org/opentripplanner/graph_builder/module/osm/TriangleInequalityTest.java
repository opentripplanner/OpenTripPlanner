package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.graph_builder.DataImportIssueStore.noopIssueStore;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE;
import static org.opentripplanner.routing.api.request.StreetMode.CAR;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.FileDataSource;
import org.opentripplanner.graph_builder.ConfiguredDataSource;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmDefaultParameters;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractParameters;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractParametersBuilder;
import org.opentripplanner.openstreetmap.OpenStreetMapProvider;
import org.opentripplanner.routing.algorithm.astar.AStarBuilder;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.core.intersection_model.ConstantIntersectionTraversalCalculator;
import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCalculator;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.transit.model.framework.Deduplicator;

public class TriangleInequalityTest {

  private static Graph graph;

  private final IntersectionTraversalCalculator calculator = new ConstantIntersectionTraversalCalculator(
    10.0
  );

  private Vertex start;
  private Vertex end;

  @BeforeAll
  public static void onlyOnce() {
    graph = new Graph(new Deduplicator());

    File file = new File(
      URLDecoder.decode(
        TriangleInequalityTest.class.getResource("NYC_small.osm.pbf").getFile(),
        StandardCharsets.UTF_8
      )
    );
    FileDataSource dataSource = new FileDataSource(file, FileType.OSM);
    ConfiguredDataSource<OsmExtractParameters> source = new ConfiguredDataSource<>(
      dataSource,
      new OsmExtractParametersBuilder().withSource(dataSource.uri()).build()
    );
    OpenStreetMapProvider provider = new OpenStreetMapProvider(
      source,
      new OsmDefaultParameters(),
      true
    );

    OpenStreetMapModule osmModule = new OpenStreetMapModule(
      List.of(provider),
      Set.of(),
      graph,
      null,
      noopIssueStore()
    );
    osmModule.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
    osmModule.buildGraph();
  }

  @BeforeEach
  public void before() {
    start = graph.getVertex("osm:node:1919595913");
    end = graph.getVertex("osm:node:42448554");
  }

  @Test
  public void testTriangleInequalityDefaultModes() {
    checkTriangleInequality();
  }

  @Test
  public void testTriangleInequalityWalkingOnly() {
    RequestModes modes = RequestModes.of().clearTransitModes().build();
    checkTriangleInequality(modes);
  }

  @Test
  public void testTriangleInequalityDrivingOnly() {
    RequestModes modes = RequestModes.of().withDirectMode(CAR).clearTransitModes().build();
    checkTriangleInequality(modes);
  }

  @Test
  public void testTriangleInequalityWalkTransit() {
    RequestModes modes = RequestModes.defaultRequestModes();
    checkTriangleInequality(modes);
  }

  @Test
  public void testTriangleInequalityWalkBike() {
    RequestModes modes = RequestModes.of().withDirectMode(BIKE).clearTransitModes().build();
    checkTriangleInequality(modes);
  }

  @Test
  public void testTriangleInequalityDefaultModesBasicSPT() {
    checkTriangleInequality(null);
  }

  @Test
  public void testTriangleInequalityWalkingOnlyBasicSPT() {
    RequestModes modes = RequestModes.of().clearTransitModes().build();
    checkTriangleInequality(modes);
  }

  @Test
  public void testTriangleInequalityDrivingOnlyBasicSPT() {
    RequestModes modes = RequestModes.of().withDirectMode(CAR).clearTransitModes().build();
    checkTriangleInequality(modes);
  }

  @Test
  public void testTriangleInequalityWalkTransitBasicSPT() {
    RequestModes modes = RequestModes.defaultRequestModes();
    checkTriangleInequality(modes);
  }

  @Test
  public void testTriangleInequalityWalkBikeBasicSPT() {
    RequestModes modes = RequestModes.of().withDirectMode(BIKE).clearTransitModes().build();
    checkTriangleInequality(modes);
  }

  @Test
  public void testTriangleInequalityDefaultModesMultiSPT() {
    checkTriangleInequality(null);
  }

  @Test
  public void testTriangleInequalityWalkingOnlyMultiSPT() {
    RequestModes modes = RequestModes.of().clearTransitModes().build();
    checkTriangleInequality(modes);
  }

  @Test
  public void testTriangleInequalityDrivingOnlyMultiSPT() {
    RequestModes modes = RequestModes.of().withDirectMode(CAR).clearTransitModes().build();
    checkTriangleInequality(modes);
  }

  @Test
  public void testTriangleInequalityWalkTransitMultiSPT() {
    RequestModes modes = RequestModes.defaultRequestModes();
    checkTriangleInequality(modes);
  }

  @Test
  public void testTriangleInequalityWalkBikeMultiSPT() {
    RequestModes modes = RequestModes.of().withDirectMode(BIKE).clearTransitModes().build();
    checkTriangleInequality(modes);
  }

  private GraphPath getPath(RouteRequest options, Edge startBackEdge, Vertex u, Vertex v) {
    return AStarBuilder
      .oneToOne()
      .setOriginBackEdge(startBackEdge)
      .setRequest(options)
      .setFrom(u)
      .setTo(v)
      .setIntersectionTraversalCalculator(calculator)
      .getShortestPathTree()
      .getPath(v);
  }

  private void checkTriangleInequality() {
    checkTriangleInequality(null);
  }

  private void checkTriangleInequality(RequestModes modes) {
    assertNotNull(start);
    assertNotNull(end);

    RouteRequest prototypeOptions = new RouteRequest();

    // All reluctance terms are 1.0 so that duration is monotonically increasing in weight.
    prototypeOptions.withPreferences(preferences ->
      preferences
        .withWalk(walk -> walk.withStairsReluctance(1.0).withSpeed(1.0).withReluctance(1.0))
        .withStreet(street -> street.withTurnReluctance(1.0))
        .withCar(car -> car.withSpeed(1.0).withReluctance(1.0))
        .withBike(bike -> bike.withSpeed(1.0).withReluctance(1.0))
    );

    if (modes != null) {
      prototypeOptions.journey().setModes(modes);
    }

    ShortestPathTree tree = AStarBuilder
      .oneToOne()
      .setDominanceFunction(new DominanceFunction.EarliestArrival())
      .setRequest(prototypeOptions)
      .setFrom(start)
      .setTo(end)
      .setIntersectionTraversalCalculator(calculator)
      .getShortestPathTree();

    GraphPath path = tree.getPath(end);
    assertNotNull(path);

    double startEndWeight = path.getWeight();
    int startEndDuration = path.getDuration();
    assertTrue(startEndWeight > 0);
    assertEquals(startEndWeight, startEndDuration, 1.0 * path.edges.size());

    // Try every vertex in the graph as an intermediate.
    boolean violated = false;
    for (Vertex intermediate : graph.getVertices()) {
      if (intermediate == start || intermediate == end) {
        continue;
      }

      GraphPath startIntermediatePath = getPath(prototypeOptions, null, start, intermediate);
      if (startIntermediatePath == null) {
        continue;
      }

      Edge back = startIntermediatePath.states.getLast().getBackEdge();
      GraphPath intermediateEndPath = getPath(prototypeOptions, back, intermediate, end);
      if (intermediateEndPath == null) {
        continue;
      }

      double startIntermediateWeight = startIntermediatePath.getWeight();
      int startIntermediateDuration = startIntermediatePath.getDuration();
      double intermediateEndWeight = intermediateEndPath.getWeight();
      int intermediateEndDuration = intermediateEndPath.getDuration();

      // TODO(flamholz): fix traversal so that there's no rounding at the second resolution.
      assertEquals(
        startIntermediateWeight,
        startIntermediateDuration,
        1.0 * startIntermediatePath.edges.size()
      );
      assertEquals(
        intermediateEndWeight,
        intermediateEndDuration,
        1.0 * intermediateEndPath.edges.size()
      );

      double diff = startIntermediateWeight + intermediateEndWeight - startEndWeight;
      if (diff < -0.01) {
        System.out.println("Triangle inequality violated - diff = " + diff);
        violated = true;
      }
      //assertTrue(startIntermediateDuration + intermediateEndDuration >=
      //        startEndDuration);
    }

    assertFalse(violated);
  }
}
