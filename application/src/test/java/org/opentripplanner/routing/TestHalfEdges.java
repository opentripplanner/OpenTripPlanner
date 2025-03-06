package org.opentripplanner.routing;

import static com.google.common.collect.Iterables.filter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.linearref.LinearLocation;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.module.TestStreetLinkerModule;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.index.StreetIndex;
import org.opentripplanner.routing.graphfinder.DirectGraphFinder;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.linking.SameEdgeAdjuster;
import org.opentripplanner.routing.services.notes.StreetNotesService;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.note.StreetNote;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexFactory;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.StreetSearchRequestBuilder;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.TimetableRepository;

public class TestHalfEdges {

  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();

  private Graph graph;
  private StreetEdge top, bottom, left, right, leftBack, rightBack;
  private IntersectionVertex br, tr, bl, tl;
  private TransitStopVertex station1;
  private TransitStopVertex station2;
  private TimetableRepository timetableRepository;

  @BeforeEach
  public void setUp() {
    var deduplicator = new Deduplicator();
    graph = new Graph(deduplicator);
    var siteRepositoryBuilder = testModel.siteRepositoryBuilder();
    var factory = new VertexFactory(graph);
    // a 0.1 degree x 0.1 degree square
    tl = factory.intersection("tl", -74.01, 40.01);
    tr = factory.intersection("tr", -74.0, 40.01);
    bl = factory.intersection("bl", -74.01, 40.0);
    br = factory.intersection("br", -74.00, 40.0);

    top = new StreetEdgeBuilder<>()
      .withFromVertex(tl)
      .withToVertex(tr)
      .withGeometry(GeometryUtils.makeLineString(-74.01, 40.01, -74.0, 40.01))
      .withName("top")
      .withMeterLength(1500)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(false)
      .buildAndConnect();
    bottom = new StreetEdgeBuilder<>()
      .withFromVertex(br)
      .withToVertex(bl)
      .withGeometry(GeometryUtils.makeLineString(-74.01, 40.0, -74.0, 40.0))
      .withName("bottom")
      .withMeterLength(1500)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(false)
      .buildAndConnect();
    left = new StreetEdgeBuilder<>()
      .withFromVertex(bl)
      .withToVertex(tl)
      .withGeometry(GeometryUtils.makeLineString(-74.01, 40.0, -74.01, 40.01))
      .withName("left")
      .withMeterLength(1500)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(false)
      .buildAndConnect();
    right = new StreetEdgeBuilder<>()
      .withFromVertex(br)
      .withToVertex(tr)
      .withGeometry(GeometryUtils.makeLineString(-74.0, 40.0, -74.0, 40.01))
      .withName("right")
      .withMeterLength(1500)
      .withPermission(StreetTraversalPermission.PEDESTRIAN)
      .withBack(false)
      .buildAndConnect();

    @SuppressWarnings("unused")
    StreetEdge topBack = new StreetEdgeBuilder<>()
      .withFromVertex(tr)
      .withToVertex(tl)
      .withGeometry(top.getGeometry().reverse())
      .withName("topBack")
      .withMeterLength(1500)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(true)
      .buildAndConnect();
    @SuppressWarnings("unused")
    StreetEdge bottomBack = new StreetEdgeBuilder<>()
      .withFromVertex(br)
      .withToVertex(bl)
      .withGeometry(bottom.getGeometry().reverse())
      .withName("bottomBack")
      .withMeterLength(1500)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(true)
      .buildAndConnect();
    leftBack = new StreetEdgeBuilder<>()
      .withFromVertex(tl)
      .withToVertex(bl)
      .withGeometry(left.getGeometry().reverse())
      .withName("leftBack")
      .withMeterLength(1500)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(true)
      .buildAndConnect();
    rightBack = new StreetEdgeBuilder<>()
      .withFromVertex(tr)
      .withToVertex(br)
      .withGeometry(right.getGeometry().reverse())
      .withName("rightBack")
      .withMeterLength(1500)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(true)
      .buildAndConnect();

    var s1 = testModel.stop("fleem station", 40.0099999, -74.005).build();
    var s2 = testModel.stop("morx station", 40.0099999, -74.002).build();

    siteRepositoryBuilder.withRegularStop(s1).withRegularStop(s2);
    timetableRepository = new TimetableRepository(siteRepositoryBuilder.build(), deduplicator);

    station1 = factory.transitStop(TransitStopVertex.of().withStop(s1));
    station2 = factory.transitStop(TransitStopVertex.of().withStop(s2));
    station1.addMode(TransitMode.RAIL);
    station2.addMode(TransitMode.RAIL);

    //Linkers aren't run otherwise in testNetworkLinker
    graph.hasStreets = true;

    timetableRepository.index();
    graph.index(timetableRepository.getSiteRepository());
  }

  @Test
  public void testHalfEdges() {
    // the shortest half-edge from the start vertex takes you down, but the shortest total path
    // is up and over

    DisposableEdgeCollection tempEdges = new DisposableEdgeCollection(graph);

    int nVertices = graph.getVertices().size();
    int nEdges = graph.getEdges().size();

    RouteRequest options = new RouteRequest();

    HashSet<Edge> turns = new HashSet<>();
    turns.add(left);
    turns.add(leftBack);

    TemporaryStreetLocation start = StreetIndex.createTemporaryStreetLocationForTest(
      "start",
      new NonLocalizedString("start"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()),
      false,
      tempEdges
    );

    HashSet<Edge> endTurns = new HashSet<>();
    endTurns.add(right);
    endTurns.add(rightBack);

    TemporaryStreetLocation end = StreetIndex.createTemporaryStreetLocationForTest(
      "end",
      new NonLocalizedString("end"),
      filter(endTurns, StreetEdge.class),
      new LinearLocation(0, 0.8).getCoordinate(right.getGeometry()),
      true,
      tempEdges
    );

    assertTrue(start.getX() < end.getX());
    assertTrue(start.getY() < end.getY());

    Collection<Edge> edges = end.getIncoming();

    assertEquals(2, edges.size());

    long startTime = LocalDateTime.of(2009, Month.DECEMBER, 1, 12, 34, 25)
      .atZone(ZoneIds.NEW_YORK)
      .toEpochSecond();
    options.setDateTime(Instant.ofEpochSecond(startTime));
    ShortestPathTree<State, Edge, Vertex> spt1 = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(options)
      .setStreetRequest(options.journey().direct())
      .setFrom(br)
      .setTo(end)
      .getShortestPathTree();

    GraphPath<State, Edge, Vertex> pathBr = spt1.getPath(end);
    assertNotNull(pathBr, "There must be a path from br to end");

    ShortestPathTree<State, Edge, Vertex> spt2 = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(options)
      .setStreetRequest(options.journey().direct())
      .setFrom(tr)
      .setTo(end)
      .getShortestPathTree();

    GraphPath<State, Edge, Vertex> pathTr = spt2.getPath(end);
    assertNotNull(pathTr, "There must be a path from tr to end");
    assertTrue(
      pathBr.getWeight() > pathTr.getWeight(),
      "path from bottom to end must be longer than path from top to end"
    );

    ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(options)
      .setStreetRequest(options.journey().direct())
      .setFrom(start)
      .setTo(end)
      .getShortestPathTree();

    GraphPath<State, Edge, Vertex> path = spt.getPath(end);
    assertNotNull(path, "There must be a path from start to end");

    // the bottom is not part of the shortest path
    for (State s : path.states) {
      assertNotSame(s.getVertex(), graph.getVertex("bottom"));
      assertNotSame(s.getVertex(), graph.getVertex("bottomBack"));
    }

    options.setArriveBy(true);
    spt = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(options)
      .setStreetRequest(options.journey().direct())
      .setFrom(start)
      .setTo(end)
      .getShortestPathTree();

    path = spt.getPath(start);
    assertNotNull(path, "There must be a path from start to end (looking back)");

    // the bottom edge is not part of the shortest path
    for (State s : path.states) {
      assertNotSame(s.getVertex(), graph.getVertex("bottom"));
      assertNotSame(s.getVertex(), graph.getVertex("bottomBack"));
    }

    // Number of vertices and edges should be the same as before after a cleanup.
    tempEdges.disposeEdges();
    assertEquals(nVertices, graph.getVertices().size());
    assertEquals(nEdges, graph.getEdges().size());

    /*
     * Now, the right edge is not bikeable. But the user can walk their bike. So here are some tests that prove (a) that walking bikes works, but
     * that (b) it is not preferred to riding a tiny bit longer.
     */

    options = new RouteRequest();
    options.journey().direct().setMode(StreetMode.BIKE);
    start = StreetIndex.createTemporaryStreetLocationForTest(
      "start1",
      new NonLocalizedString("start1"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.95).getCoordinate(top.getGeometry()),
      false,
      tempEdges
    );
    end = StreetIndex.createTemporaryStreetLocationForTest(
      "end1",
      new NonLocalizedString("end1"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.95).getCoordinate(bottom.getGeometry()),
      true,
      tempEdges
    );

    spt = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(options)
      .setStreetRequest(options.journey().direct())
      .setFrom(start)
      .setTo(end)
      .getShortestPathTree();

    path = spt.getPath(start);
    assertNotNull(path, "There must be a path from top to bottom along the right");

    // the left edge is not part of the shortest path (even though the bike must be walked along the right)
    for (State s : path.states) {
      assertNotSame(s.getVertex(), graph.getVertex("left"));
      assertNotSame(s.getVertex(), graph.getVertex("leftBack"));
    }

    // Number of vertices and edges should be the same as before after a cleanup.
    tempEdges.disposeEdges();
    assertEquals(nVertices, graph.getVertices().size());
    assertEquals(nEdges, graph.getEdges().size());

    start = StreetIndex.createTemporaryStreetLocationForTest(
      "start2",
      new NonLocalizedString("start2"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.55).getCoordinate(top.getGeometry()),
      false,
      tempEdges
    );
    end = StreetIndex.createTemporaryStreetLocationForTest(
      "end2",
      new NonLocalizedString("end2"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.55).getCoordinate(bottom.getGeometry()),
      true,
      tempEdges
    );

    spt = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(options)
      .setFrom(start)
      .setTo(end)
      .getShortestPathTree();

    path = spt.getPath(start);
    assertNotNull(path, "There must be a path from top to bottom");

    // the right edge is not part of the shortest path, e
    for (State s : path.states) {
      assertNotSame(s.getVertex(), graph.getVertex("right"));
      assertNotSame(s.getVertex(), graph.getVertex("rightBack"));
    }

    // Number of vertices and edges should be the same as before after a cleanup.
    tempEdges.disposeEdges();
    assertEquals(nVertices, graph.getVertices().size());
    assertEquals(nEdges, graph.getEdges().size());
  }

  @Test
  public void testRouteToSameEdge() {
    RouteRequest options = new RouteRequest();
    DisposableEdgeCollection tempEdges = new DisposableEdgeCollection(graph);

    HashSet<Edge> turns = new HashSet<>();
    turns.add(left);
    turns.add(leftBack);

    TemporaryStreetLocation start = StreetIndex.createTemporaryStreetLocationForTest(
      "start",
      new NonLocalizedString("start"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()),
      false,
      tempEdges
    );

    TemporaryStreetLocation end = StreetIndex.createTemporaryStreetLocationForTest(
      "end",
      new NonLocalizedString("end"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.8).getCoordinate(left.getGeometry()),
      true,
      tempEdges
    );

    DisposableEdgeCollection connection = SameEdgeAdjuster.adjust(start, end, graph);

    assertEquals(start.getX(), end.getX(), 0.0001);
    assertTrue(start.getY() < end.getY());

    Collection<Edge> edges = end.getIncoming();

    assertEquals(3, edges.size());

    long startTime = LocalDateTime.of(2009, Month.DECEMBER, 1, 12, 34, 25)
      .atZone(ZoneIds.NEW_YORK)
      .toEpochSecond();
    options.setDateTime(Instant.ofEpochSecond(startTime));
    ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(options)
      .setStreetRequest(options.journey().direct())
      .setFrom(start)
      .setTo(end)
      .getShortestPathTree();

    GraphPath<State, Edge, Vertex> path = spt.getPath(end);
    assertNotNull(path, "There must be a path from start to end");
    assertEquals(1, path.edges.size());
    tempEdges.disposeEdges();
    connection.disposeEdges();
  }

  @Test
  public void testRouteToSameEdgeBackwards() {
    RouteRequest options = new RouteRequest();
    DisposableEdgeCollection tempEdges = new DisposableEdgeCollection(graph);

    // Sits only on the leftmost edge, not on its reverse.
    HashSet<Edge> turns = new HashSet<>();
    turns.add(left);

    TemporaryStreetLocation start = StreetIndex.createTemporaryStreetLocationForTest(
      "start",
      new NonLocalizedString("start"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.8).getCoordinate(left.getGeometry()),
      false,
      tempEdges
    );

    TemporaryStreetLocation end = StreetIndex.createTemporaryStreetLocationForTest(
      "end",
      new NonLocalizedString("end"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()),
      true,
      tempEdges
    );

    DisposableEdgeCollection connection = SameEdgeAdjuster.adjust(start, end, graph);

    assertEquals(start.getX(), end.getX(), 0.001);
    assertTrue(start.getY() > end.getY());

    Collection<Edge> edges = end.getIncoming();
    assertEquals(1, edges.size());

    long startTime = LocalDateTime.of(2009, Month.DECEMBER, 1, 12, 34, 25)
      .atZone(ZoneIds.NEW_YORK)
      .toEpochSecond();
    options.setDateTime(Instant.ofEpochSecond(startTime));
    ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(options)
      .setStreetRequest(options.journey().direct())
      .setFrom(start)
      .setTo(end)
      .getShortestPathTree();

    GraphPath<State, Edge, Vertex> path = spt.getPath(end);
    assertNotNull(path, "There must be a path from start to end");
    assertTrue(path.edges.size() > 1);
    tempEdges.disposeEdges();
    connection.disposeEdges();
  }

  /**
   * Test that alerts on split streets are preserved, i.e. if there are alerts on the street that is
   * split the same alerts should be present on the new street.
   */
  @Test
  public void testStreetSplittingAlerts() {
    DisposableEdgeCollection tempEdges = new DisposableEdgeCollection(graph);

    HashSet<Edge> turns = new HashSet<>();
    turns.add(left);
    turns.add(leftBack);

    StreetNote alert = new StreetNote("This is the alert");
    Set<StreetNote> alerts = new HashSet<>();
    alerts.add(alert);

    graph.streetNotesService.addStaticNote(left, alert, StreetNotesService.ALWAYS_MATCHER);
    graph.streetNotesService.addStaticNote(leftBack, alert, StreetNotesService.ALWAYS_MATCHER);

    TemporaryStreetLocation start = StreetIndex.createTemporaryStreetLocationForTest(
      "start",
      new NonLocalizedString("start"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()),
      false,
      tempEdges
    );

    // The alert should be preserved
    // traverse the FreeEdge from the StreetLocation to the new IntersectionVertex
    StreetSearchRequestBuilder req = StreetSearchRequest.of();
    State traversedOne = new State(start, req.build());
    for (Edge e : start.getOutgoing()) {
      var states = e.traverse(traversedOne);
      if (!State.isEmpty(states)) {
        traversedOne = states[0];
        break;
      }
    }

    assertEquals(alerts, graph.streetNotesService.getNotes(traversedOne));
    assertNotSame(left, traversedOne.getBackEdge().getFromVertex());
    assertNotSame(leftBack, traversedOne.getBackEdge().getFromVertex());

    // now, make sure wheelchair alerts are preserved
    StreetNote wheelchairAlert = new StreetNote("This is the wheelchair alert");
    Set<StreetNote> wheelchairAlerts = new HashSet<>();
    wheelchairAlerts.add(wheelchairAlert);

    graph.streetNotesService.removeStaticNotes(left);
    graph.streetNotesService.removeStaticNotes(leftBack);
    graph.streetNotesService.addStaticNote(
      left,
      wheelchairAlert,
      StreetNotesService.WHEELCHAIR_MATCHER
    );
    graph.streetNotesService.addStaticNote(
      leftBack,
      wheelchairAlert,
      StreetNotesService.WHEELCHAIR_MATCHER
    );

    req.withWheelchair(true);

    start = StreetIndex.createTemporaryStreetLocationForTest(
      "start",
      new NonLocalizedString("start"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()),
      false,
      tempEdges
    );

    traversedOne = new State(start, req.build());
    for (Edge e : start.getOutgoing()) {
      var states = e.traverse(traversedOne);
      if (!State.isEmpty(states)) {
        traversedOne = states[0];
        break;
      }
    }

    assertEquals(wheelchairAlerts, graph.streetNotesService.getNotes(traversedOne));
    assertNotSame(left, traversedOne.getBackEdge().getFromVertex());
    assertNotSame(leftBack, traversedOne.getBackEdge().getFromVertex());
    tempEdges.disposeEdges();
  }

  @Test
  public void testStreetLocationFinder() {
    StreetIndex finder = graph.getStreetIndex();
    GraphFinder graphFinder = new DirectGraphFinder(
      timetableRepository.getSiteRepository()::findRegularStops
    );
    Set<DisposableEdgeCollection> tempEdges = new HashSet<>();
    // test that the local stop finder finds stops
    assertTrue(graphFinder.findClosestStops(new Coordinate(-74.005000001, 40.01), 100).size() > 0);

    // test that the closest vertex finder returns the closest vertex
    TemporaryStreetLocation some =
      (TemporaryStreetLocation) finder.createVertexForCoordinateForTest(
        new Coordinate(-74.00, 40.00),
        StreetMode.WALK,
        true,
        tempEdges
      );
    assertNotNull(some);

    // test that the closest vertex finder correctly splits streets
    TemporaryStreetLocation start =
      (TemporaryStreetLocation) finder.createVertexForCoordinateForTest(
        new Coordinate(-74.01, 40.004),
        StreetMode.WALK,
        false,
        tempEdges
      );
    assertNotNull(start);
    assertTrue(
      start.isWheelchairAccessible(),
      "wheelchair accessibility is correctly set (splitting)"
    );

    Collection<Edge> edges = start.getOutgoing();
    assertEquals(2, edges.size());

    TemporaryStreetLocation end = (TemporaryStreetLocation) finder.createVertexForCoordinateForTest(
      new Coordinate(-74.0, 40.008),
      StreetMode.BIKE,
      true,
      tempEdges
    );
    assertNotNull(end);

    edges = end.getIncoming();
    assertEquals(2, edges.size());

    tempEdges.forEach(DisposableEdgeCollection::disposeEdges);
  }

  @Test
  public void testTemporaryVerticesContainer() {
    // test that it is possible to travel between two splits on the same street
    RouteRequest walking = new RouteRequest();
    walking.setFrom(new GenericLocation(40.004, -74.0));
    walking.setTo(new GenericLocation(40.008, -74.0));
    try (
      var container = new TemporaryVerticesContainer(
        graph,
        walking.from(),
        walking.to(),
        StreetMode.WALK,
        StreetMode.WALK
      )
    ) {
      assertNotNull(container.getFromVertices());
      assertNotNull(container.getToVertices());
      ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder.of()
        .setHeuristic(new EuclideanRemainingWeightHeuristic())
        .setRequest(walking)
        .setVerticesContainer(container)
        .getShortestPathTree();
      GraphPath<State, Edge, Vertex> path = spt.getPath(
        container.getToVertices().iterator().next()
      );
      for (State s : path.states) {
        assertNotSame(s.getBackEdge(), top);
      }
    }
  }

  @Test
  public void testNetworkLinker() {
    int numVerticesBefore = graph.getVertices().size();
    TestStreetLinkerModule.link(graph, timetableRepository);
    int numVerticesAfter = graph.getVertices().size();
    assertEquals(4, numVerticesAfter - numVerticesBefore);
    Collection<Edge> outgoing = station1.getOutgoing();
    assertEquals(2, outgoing.size());
    Edge edge = outgoing.iterator().next();

    Vertex midpoint = edge.getToVertex();
    assertTrue(Math.abs(midpoint.getCoordinate().y - 40.01) < 0.00000001);

    outgoing = station2.getOutgoing();
    assertEquals(2, outgoing.size());
    edge = outgoing.iterator().next();

    Vertex station2point = edge.getToVertex();
    assertTrue(Math.abs(station2point.getCoordinate().x - -74.002) < 0.00000001);
  }
}
