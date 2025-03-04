package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE;
import static org.opentripplanner.routing.api.request.StreetMode.CAR;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.model.modes.ExcludeAllTransitFilter;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.filter.AllowAllTransitFilter;
import org.opentripplanner.routing.api.request.request.filter.TransitFilter;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.intersection_model.ConstantIntersectionTraversalCalculator;
import org.opentripplanner.street.search.intersection_model.IntersectionTraversalCalculator;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.street.search.strategy.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.framework.Deduplicator;

public class TriangleInequalityTest {

  private static Graph graph;

  private final IntersectionTraversalCalculator calculator =
    new ConstantIntersectionTraversalCalculator(10.0);

  private Vertex start;
  private Vertex end;

  @BeforeAll
  public static void onlyOnce() {
    graph = new Graph(new Deduplicator());

    File file = ResourceLoader.of(TriangleInequalityTest.class).file("NYC_small.osm.pbf");
    DefaultOsmProvider provider = new DefaultOsmProvider(file, true);
    OsmModule osmModule = OsmModule.of(
      provider,
      graph,
      new DefaultOsmInfoGraphBuildRepository(),
      new DefaultVehicleParkingRepository()
    )
      .withAreaVisibility(true)
      .build();
    osmModule.buildGraph();
  }

  @BeforeEach
  public void before() {
    start = graph.getVertex(VertexLabel.osm(1919595913));
    end = graph.getVertex(VertexLabel.osm(42448554));
  }

  @Test
  public void testTriangleInequalityDefaultModes() {
    checkTriangleInequality();
  }

  @Test
  public void testTriangleInequalityWalkingOnly() {
    RequestModes modes = RequestModes.of().build();
    checkTriangleInequality(modes, List.of(ExcludeAllTransitFilter.of()));
  }

  @Test
  public void testTriangleInequalityDrivingOnly() {
    RequestModes modes = RequestModes.of().withDirectMode(CAR).build();
    checkTriangleInequality(modes, List.of(ExcludeAllTransitFilter.of()));
  }

  @Test
  public void testTriangleInequalityWalkTransit() {
    RequestModes modes = RequestModes.defaultRequestModes();
    checkTriangleInequality(modes, List.of(AllowAllTransitFilter.of()));
  }

  @Test
  public void testTriangleInequalityWalkBike() {
    RequestModes modes = RequestModes.of().withDirectMode(BIKE).build();
    checkTriangleInequality(modes, List.of(ExcludeAllTransitFilter.of()));
  }

  @Test
  public void testTriangleInequalityDefaultModesBasicSPT() {
    checkTriangleInequality(null, List.of());
  }

  @Test
  public void testTriangleInequalityWalkingOnlyBasicSPT() {
    RequestModes modes = RequestModes.of().build();
    checkTriangleInequality(modes, List.of(ExcludeAllTransitFilter.of()));
  }

  @Test
  public void testTriangleInequalityDrivingOnlyBasicSPT() {
    RequestModes modes = RequestModes.of().withDirectMode(CAR).build();
    checkTriangleInequality(modes, List.of(ExcludeAllTransitFilter.of()));
  }

  @Test
  public void testTriangleInequalityWalkTransitBasicSPT() {
    RequestModes modes = RequestModes.defaultRequestModes();
    checkTriangleInequality(modes, List.of(AllowAllTransitFilter.of()));
  }

  @Test
  public void testTriangleInequalityWalkBikeBasicSPT() {
    RequestModes modes = RequestModes.of().withDirectMode(BIKE).build();
    checkTriangleInequality(modes, List.of(ExcludeAllTransitFilter.of()));
  }

  @Test
  public void testTriangleInequalityDefaultModesMultiSPT() {
    checkTriangleInequality(null, List.of());
  }

  @Test
  public void testTriangleInequalityWalkingOnlyMultiSPT() {
    RequestModes modes = RequestModes.of().build();
    checkTriangleInequality(modes, List.of(ExcludeAllTransitFilter.of()));
  }

  @Test
  public void testTriangleInequalityDrivingOnlyMultiSPT() {
    RequestModes modes = RequestModes.of().withDirectMode(CAR).build();
    checkTriangleInequality(modes, List.of(ExcludeAllTransitFilter.of()));
  }

  @Test
  public void testTriangleInequalityWalkTransitMultiSPT() {
    RequestModes modes = RequestModes.defaultRequestModes();
    checkTriangleInequality(modes, List.of(AllowAllTransitFilter.of()));
  }

  @Test
  public void testTriangleInequalityWalkBikeMultiSPT() {
    RequestModes modes = RequestModes.of().withDirectMode(BIKE).build();
    checkTriangleInequality(modes, List.of(ExcludeAllTransitFilter.of()));
  }

  private GraphPath<State, Edge, Vertex> getPath(
    RouteRequest options,
    Edge startBackEdge,
    Vertex u,
    Vertex v
  ) {
    return StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setOriginBackEdge(startBackEdge)
      .setRequest(options)
      .setFrom(u)
      .setTo(v)
      .setIntersectionTraversalCalculator(calculator)
      .getShortestPathTree()
      .getPath(v);
  }

  private void checkTriangleInequality() {
    checkTriangleInequality(null, List.of());
  }

  private void checkTriangleInequality(RequestModes modes, List<TransitFilter> filters) {
    assertNotNull(start);
    assertNotNull(end);

    RouteRequest prototypeOptions = new RouteRequest();

    // All reluctance terms are 1.0 so that duration is monotonically increasing in weight.
    prototypeOptions.withPreferences(preferences ->
      preferences
        .withWalk(walk -> walk.withStairsReluctance(1.0).withSpeed(1.0).withReluctance(1.0))
        .withStreet(street -> street.withTurnReluctance(1.0))
        .withCar(car -> car.withReluctance(1.0))
        .withBike(bike -> bike.withSpeed(1.0).withReluctance(1.0))
        .withScooter(scooter -> scooter.withSpeed(1.0).withReluctance(1.0))
    );

    if (modes != null) {
      prototypeOptions.journey().setModes(modes);
    }
    if (!filters.isEmpty()) {
      prototypeOptions.journey().transit().setFilters(filters);
    }

    ShortestPathTree<State, Edge, Vertex> tree = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setDominanceFunction(new DominanceFunctions.EarliestArrival())
      .setRequest(prototypeOptions)
      .setFrom(start)
      .setTo(end)
      .setIntersectionTraversalCalculator(calculator)
      .getShortestPathTree();

    GraphPath<State, Edge, Vertex> path = tree.getPath(end);
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

      GraphPath<State, Edge, Vertex> startIntermediatePath = getPath(
        prototypeOptions,
        null,
        start,
        intermediate
      );
      if (startIntermediatePath == null) {
        continue;
      }

      Edge back = startIntermediatePath.states.getLast().getBackEdge();
      GraphPath<State, Edge, Vertex> intermediateEndPath = getPath(
        prototypeOptions,
        back,
        intermediate,
        end
      );
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
