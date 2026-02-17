package org.opentripplanner.street.integration;

import static com.google.common.collect.Iterables.filter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.linearref.LinearLocation;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.DisposableEdgeCollection;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.SameEdgeAdjuster;
import org.opentripplanner.routing.linking.TemporaryVerticesContainer;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.routing.linking.mapping.LinkingContextRequestMapper;
import org.opentripplanner.routing.services.notes.StreetNotesService;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.note.StreetNote;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.StreetSearchRequestBuilder;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.streetadapter.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.streetadapter.StreetSearchBuilder;
import org.opentripplanner.streetadapter.VertexFactory;

public class EdgeSplittingTest {

  private Graph graph;
  private StreetEdge top;
  private StreetEdge bottom;
  private StreetEdge left;
  private StreetEdge right;
  private StreetEdge leftBack;
  private IntersectionVertex br;
  private IntersectionVertex tr;
  private IntersectionVertex bl;
  private IntersectionVertex tl;

  @BeforeEach
  public void setUp() {
    graph = new Graph();
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

    new StreetEdgeBuilder<>()
      .withFromVertex(tr)
      .withToVertex(tl)
      .withGeometry(top.getGeometry().reverse())
      .withName("topBack")
      .withMeterLength(1500)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(true)
      .buildAndConnect();
    new StreetEdgeBuilder<>()
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

    //Linkers aren't run otherwise in testNetworkLinker
    graph.hasStreets = true;

    graph.index();
  }

  @Test
  public void testRouteToSameEdge() {
    HashSet<Edge> turns = new HashSet<>();
    turns.add(left);
    turns.add(leftBack);

    TemporaryStreetLocation start = StreetModelForTest.createTemporaryStreetLocationForTest(
      new NonLocalizedString("start"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()),
      false
    );

    TemporaryStreetLocation end = StreetModelForTest.createTemporaryStreetLocationForTest(
      new NonLocalizedString("end"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.8).getCoordinate(left.getGeometry()),
      true
    );

    DisposableEdgeCollection connection = SameEdgeAdjuster.adjust(start, end, graph);

    assertEquals(start.getX(), end.getX(), 0.0001);
    assertTrue(start.getY() < end.getY());

    Collection<Edge> edges = end.getIncoming();

    assertEquals(3, edges.size());

    var request = RouteRequest.defaultValue();

    ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder.of()
      .withHeuristic(new EuclideanRemainingWeightHeuristic())
      .withRequest(request)
      .withStreetRequest(request.journey().direct())
      .withFrom(start)
      .withTo(end)
      .getShortestPathTree();

    GraphPath<State, Edge, Vertex> path = spt.getPath(end);
    assertNotNull(path, "There must be a path from start to end");
    assertEquals(1, path.edges.size());
    connection.disposeEdges();
  }

  @Test
  public void testRouteToSameEdgeBackwards() {
    // Sits only on the leftmost edge, not on its reverse.
    HashSet<Edge> turns = new HashSet<>();
    turns.add(left);

    TemporaryStreetLocation start = StreetModelForTest.createTemporaryStreetLocationForTest(
      new NonLocalizedString("start"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.8).getCoordinate(left.getGeometry()),
      false
    );

    TemporaryStreetLocation end = StreetModelForTest.createTemporaryStreetLocationForTest(
      new NonLocalizedString("end"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()),
      true
    );

    DisposableEdgeCollection connection = SameEdgeAdjuster.adjust(start, end, graph);

    assertEquals(start.getX(), end.getX(), 0.001);
    assertTrue(start.getY() > end.getY());

    Collection<Edge> edges = end.getIncoming();
    assertEquals(1, edges.size());

    var request = RouteRequest.defaultValue();

    ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder.of()
      .withHeuristic(new EuclideanRemainingWeightHeuristic())
      .withRequest(request)
      .withStreetRequest(request.journey().direct())
      .withFrom(start)
      .withTo(end)
      .getShortestPathTree();

    GraphPath<State, Edge, Vertex> path = spt.getPath(end);
    assertNotNull(path, "There must be a path from start to end");
    assertTrue(path.edges.size() > 1);
    connection.disposeEdges();
  }

  /**
   * Test that alerts on split streets are preserved, i.e. if there are alerts on the street that is
   * split the same alerts should be present on the new street.
   */
  @Test
  public void testStreetSplittingAlerts() {
    HashSet<Edge> turns = new HashSet<>();
    turns.add(left);
    turns.add(leftBack);

    StreetNote alert = new StreetNote("This is the alert");
    Set<StreetNote> alerts = new HashSet<>();
    alerts.add(alert);

    graph.streetNotesService.addStaticNote(left, alert, StreetNotesService.ALWAYS_MATCHER);
    graph.streetNotesService.addStaticNote(leftBack, alert, StreetNotesService.ALWAYS_MATCHER);

    TemporaryStreetLocation start = StreetModelForTest.createTemporaryStreetLocationForTest(
      new NonLocalizedString("start"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()),
      false
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

    req.withWheelchairEnabled(true);

    start = StreetModelForTest.createTemporaryStreetLocationForTest(
      new NonLocalizedString("start"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()),
      false
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
  }

  @Test
  public void testTemporaryVerticesContainer() {
    // test that it is possible to travel between two splits on the same street
    var from = GenericLocation.fromCoordinate(40.004, -74.0);
    var to = GenericLocation.fromCoordinate(40.008, -74.0);
    RouteRequest walking = RouteRequest.of().withFrom(from).withTo(to).buildRequest();

    try (var temporaryVerticesContainer = new TemporaryVerticesContainer()) {
      var vertexLinker = VertexLinkerTestFactory.of(graph);
      var vertexCreationService = new VertexCreationService(vertexLinker);
      var linkingContextFactory = new LinkingContextFactory(graph, vertexCreationService);
      var linkingRequest = LinkingContextRequestMapper.map(walking);
      var linkingContext = linkingContextFactory.create(temporaryVerticesContainer, linkingRequest);
      var fromVertices = linkingContext.findVertices(from);
      assertFalse(fromVertices.isEmpty());
      var toVertices = linkingContext.findVertices(to);
      assertFalse(toVertices.isEmpty());
      ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder.of()
        .withHeuristic(new EuclideanRemainingWeightHeuristic())
        .withRequest(walking)
        .withFrom(fromVertices)
        .withTo(toVertices)
        .getShortestPathTree();
      GraphPath<State, Edge, Vertex> path = spt.getPath(toVertices.iterator().next());
      for (State s : path.states) {
        assertNotSame(s.getBackEdge(), top);
      }
    }
  }

}
