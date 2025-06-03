package org.opentripplanner.astar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.vertex.VertexLabel.string;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.astar.spi.SearchTerminationStrategy;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.algorithm.MultiTargetTerminationStrategy;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model._data.SimpleConcreteEdge;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model._data.TemporaryConcreteEdge;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.EuclideanRemainingWeightHeuristic;

public class AStarTest {

  private Graph graph;

  @BeforeEach
  public void before() {
    graph = new Graph();

    vertex("56th_24th", 47.669457, -122.387577);
    vertex("56th_22nd", 47.669462, -122.384739);
    vertex("56th_20th", 47.669457, -122.382106);

    vertex("market_24th", 47.668690, -122.387577);
    vertex("market_ballard", 47.668683, -122.386096);
    vertex("market_22nd", 47.668686, -122.384749);
    vertex("market_leary", 47.668669, -122.384392);
    vertex("market_russell", 47.668655, -122.382997);
    vertex("market_20th", 47.668684, -122.382117);

    vertex("shilshole_24th", 47.668419, -122.387534);
    vertex("shilshole_22nd", 47.666519, -122.384744);
    vertex("shilshole_vernon", 47.665938, -122.384048);
    vertex("shilshole_20th", 47.664356, -122.382192);

    vertex("ballard_turn", 47.668509, -122.386069);
    vertex("ballard_22nd", 47.667624, -122.384744);
    vertex("ballard_vernon", 47.666422, -122.383158);
    vertex("ballard_20th", 47.665476, -122.382128);

    vertex("leary_vernon", 47.666863, -122.382353);
    vertex("leary_20th", 47.666682, -122.382160);

    vertex("russell_20th", 47.667846, -122.382128);

    edges("56th_24th", "56th_22nd", "56th_20th");

    edges("56th_24th", "market_24th");
    edges("56th_22nd", "market_22nd");
    edges("56th_20th", "market_20th");

    edges(
      "market_24th",
      "market_ballard",
      "market_22nd",
      "market_leary",
      "market_russell",
      "market_20th"
    );
    edges("market_24th", "shilshole_24th", "shilshole_22nd", "shilshole_vernon", "shilshole_20th");
    edges("market_ballard", "ballard_turn", "ballard_22nd", "ballard_vernon", "ballard_20th");
    edges("market_leary", "leary_vernon", "leary_20th");
    edges("market_russell", "russell_20th");

    edges("market_22nd", "ballard_22nd", "shilshole_22nd");
    edges("leary_vernon", "ballard_vernon", "shilshole_vernon");
    edges("market_20th", "russell_20th", "leary_20th", "ballard_20th", "shilshole_20th");
  }

  @Test
  public void testForward() {
    var request = new RouteRequest();

    request.withPreferences(pref -> pref.withWalk(w -> w.withSpeed(1.0)));
    Vertex from = graph.getVertex("56th_24th");
    Vertex to = graph.getVertex("leary_20th");
    ShortestPathTree tree = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(request)
      .setFrom(from)
      .setTo(to)
      .getShortestPathTree();

    GraphPath path = tree.getPath(to);

    List<State> states = path.states;

    assertEquals(7, states.size());

    assertEquals("56th_24th", states.get(0).getVertex().getLabelString());
    assertEquals("market_24th", states.get(1).getVertex().getLabelString());
    assertEquals("market_ballard", states.get(2).getVertex().getLabelString());
    assertEquals("market_22nd", states.get(3).getVertex().getLabelString());
    assertEquals("market_leary", states.get(4).getVertex().getLabelString());
    assertEquals("leary_vernon", states.get(5).getVertex().getLabelString());
    assertEquals("leary_20th", states.get(6).getVertex().getLabelString());
  }

  @Test
  public void testBack() {
    var request = new RouteRequest();

    request.withPreferences(pref -> pref.withWalk(w -> w.withSpeed(1.0)));
    request.setArriveBy(true);
    Vertex from = graph.getVertex("56th_24th");
    Vertex to = graph.getVertex("leary_20th");
    ShortestPathTree tree = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(request)
      .setFrom(from)
      .setTo(to)
      .getShortestPathTree();

    GraphPath path = tree.getPath(from);

    List<State> states = path.states;

    assertTrue(states.size() == 6 || states.size() == 7);

    assertEquals("56th_24th", states.get(0).getVertex().getLabelString());

    int n;
    // we could go either way around the block formed by 56th, 22nd, market, and 24th.
    if (states.size() == 7) {
      assertEquals("market_24th", states.get(1).getVertex().getLabelString());
      assertEquals("market_ballard", states.get(2).getVertex().getLabelString());
      n = 0;
    } else {
      assertEquals("56th_22nd", states.get(1).getVertex().getLabelString());
      n = -1;
    }

    assertEquals("market_22nd", states.get(n + 3).getVertex().getLabelString());
    assertEquals("market_leary", states.get(n + 4).getVertex().getLabelString());
    assertEquals("leary_vernon", states.get(n + 5).getVertex().getLabelString());
    assertEquals("leary_20th", states.get(n + 6).getVertex().getLabelString());
  }

  @Test
  public void testForwardExtraEdges() {
    var request = new RouteRequest();

    request.withPreferences(pref -> pref.withWalk(w -> w.withSpeed(1.0)));

    TemporaryStreetLocation from = new TemporaryStreetLocation(
      new Coordinate(-122.385050, 47.666620),
      new NonLocalizedString("near_shilshole_22nd")
    );
    TemporaryConcreteEdge.createTemporaryConcreteEdge(from, graph.getVertex("shilshole_22nd"));

    TemporaryStreetLocation to = new TemporaryStreetLocation(
      new Coordinate(-122.382347, 47.669518),
      new NonLocalizedString("near_56th_20th")
    );
    TemporaryConcreteEdge.createTemporaryConcreteEdge(graph.getVertex("56th_20th"), to);

    ShortestPathTree<State, Edge, Vertex> tree = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(request)
      .setFrom(from)
      .setTo(to)
      .getShortestPathTree();

    GraphPath<State, Edge, Vertex> path = tree.getPath(to);

    List<State> states = path.states;

    assertEquals(9, states.size());

    assertEquals("near_shilshole_22nd", states.get(0).getVertex().getDefaultName());
    assertEquals("shilshole_22nd", states.get(1).getVertex().getLabelString());
    assertEquals("ballard_22nd", states.get(2).getVertex().getLabelString());
    assertEquals("market_22nd", states.get(3).getVertex().getLabelString());
    assertEquals("market_leary", states.get(4).getVertex().getLabelString());
    assertEquals("market_russell", states.get(5).getVertex().getLabelString());
    assertEquals("market_20th", states.get(6).getVertex().getLabelString());
    assertEquals("56th_20th", states.get(7).getVertex().getLabelString());
    assertEquals("near_56th_20th", states.get(8).getVertex().getDefaultName());
  }

  @Test
  public void testBackExtraEdges() {
    var request = new RouteRequest();

    request.withPreferences(pref -> pref.withWalk(w -> w.withSpeed(1.0)));
    request.setArriveBy(true);

    TemporaryStreetLocation from = new TemporaryStreetLocation(
      new Coordinate(-122.385050, 47.666620),
      new NonLocalizedString("near_shilshole_22nd")
    );
    TemporaryConcreteEdge.createTemporaryConcreteEdge(from, graph.getVertex("shilshole_22nd"));

    TemporaryStreetLocation to = new TemporaryStreetLocation(
      new Coordinate(-122.382347, 47.669518),
      new NonLocalizedString("near_56th_20th")
    );
    TemporaryConcreteEdge.createTemporaryConcreteEdge(graph.getVertex("56th_20th"), to);

    ShortestPathTree tree = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(request)
      .setFrom(from)
      .setTo(to)
      .getShortestPathTree();

    GraphPath path = tree.getPath(from);

    List<State> states = path.states;

    assertEquals(9, states.size());

    assertEquals("near_shilshole_22nd", states.get(0).getVertex().getDefaultName());
    assertEquals("shilshole_22nd", states.get(1).getVertex().getLabelString());
    assertEquals("ballard_22nd", states.get(2).getVertex().getLabelString());
    assertEquals("market_22nd", states.get(3).getVertex().getLabelString());
    assertEquals("market_leary", states.get(4).getVertex().getLabelString());
    assertEquals("market_russell", states.get(5).getVertex().getLabelString());
    assertEquals("market_20th", states.get(6).getVertex().getLabelString());
    assertEquals("56th_20th", states.get(7).getVertex().getLabelString());
    assertEquals("near_56th_20th", states.get(8).getVertex().getDefaultName());
  }

  @Test
  public void testMultipleTargets() {
    var request = new RouteRequest();

    request.withPreferences(pref -> pref.withWalk(w -> w.withSpeed(1.0)));

    Set<Vertex> targets = new HashSet<>();
    targets.add(graph.getVertex("shilshole_22nd"));
    targets.add(graph.getVertex("market_russell"));
    targets.add(graph.getVertex("56th_20th"));
    targets.add(graph.getVertex("leary_20th"));

    SearchTerminationStrategy strategy = new MultiTargetTerminationStrategy(targets);

    Vertex v1 = graph.getVertex("56th_24th");
    Vertex v2 = graph.getVertex("leary_20th");
    ShortestPathTree tree = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setTerminationStrategy(strategy)
      .setRequest(request)
      .setFrom(v1)
      .setTo(v2)
      .getShortestPathTree();

    for (Vertex v : targets) {
      GraphPath path = tree.getPath(v);
      assertNotNull(path, "No path found for target " + v.getLabelString());
    }
  }

  /****
   * Private Methods
   ****/

  private Vertex vertex(String label, double lat, double lon) {
    var v = StreetModelForTest.intersectionVertex(label, lat, lon);
    graph.addVertex(v);
    return v;
  }

  private void edges(String... vLabels) {
    for (int i = 0; i < vLabels.length - 1; i++) {
      Vertex vA = graph.getVertex(string(vLabels[i]));
      Vertex vB = graph.getVertex(string(vLabels[i + 1]));

      SimpleConcreteEdge.createSimpleConcreteEdge(vA, vB);
      SimpleConcreteEdge.createSimpleConcreteEdge(vB, vA);
    }
  }
}
