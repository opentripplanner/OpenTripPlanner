package org.opentripplanner.routing.edgetype.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

import com.google.common.collect.Lists;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.module.geometry.GeometryAndBlockProcessor;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.routing.algorithm.astar.AStarBuilder;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.util.TestUtils;

/**
 * TODO OTP2 - Test is too close to the implementation and will need to be reimplemented.
 */
@Disabled
public class HopFactoryTest {

  private final ZoneId zoneId = ZoneId.of("America/New_York");

  private Graph graph;

  private String feedId;

  @BeforeEach
  public void setUp() throws Exception {
    GtfsContext context = contextBuilder(ConstantsForTests.FAKE_GTFS).build();
    graph = new Graph();
    GeometryAndBlockProcessor factory = new GeometryAndBlockProcessor(context);
    factory.run(graph);
    graph.putService(CalendarServiceData.class, context.getCalendarServiceData());

    feedId = context.getFeedId().getId();
  }

  @Test
  public void testDwell() {
    Vertex stop_a = graph.getVertex(feedId + ":A_depart");
    Vertex stop_c = graph.getVertex(feedId + ":C_arrive");

    RoutingRequest options = new RoutingRequest();
    options.setDateTime(ZonedDateTime.of(2009, 8, 7, 8, 0, 0, 0, zoneId).toInstant());
    ShortestPathTree spt = AStarBuilder
      .oneToOne()
      .setContext(new RoutingContext(options, graph, stop_a, stop_c))
      .getShortestPathTree();

    GraphPath path = spt.getPath(stop_c);
    assertNotNull(path);
    assertEquals(6, path.states.size());
    long endTime = ZonedDateTime.of(2009, 8, 7, 8, 30, 0, 0, zoneId).toInstant().getEpochSecond();
    assertEquals(endTime, path.getEndTime());
  }

  @Test
  public void testRouting() throws Exception {
    Vertex stop_a = graph.getVertex(feedId + ":A");
    Vertex stop_b = graph.getVertex(feedId + ":B");
    Vertex stop_c = graph.getVertex(feedId + ":C");
    Vertex stop_d = graph.getVertex(feedId + ":D");
    Vertex stop_e = graph.getVertex(feedId + ":E");

    RoutingRequest options = new RoutingRequest();
    options.setDateTime(ZonedDateTime.of(2009, 8, 7, 0, 0, 0, 0, zoneId).toInstant());

    ShortestPathTree spt;
    GraphPath path;

    // A to B
    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, graph, stop_a, stop_b))
        .getShortestPathTree();

    path = spt.getPath(stop_b);
    assertNotNull(path);
    assertEquals(extractStopVertices(path), Lists.newArrayList(stop_a, stop_b));

    // A to C
    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, graph, stop_a, stop_c))
        .getShortestPathTree();

    path = spt.getPath(stop_c);
    assertNotNull(path);
    assertEquals(extractStopVertices(path), Lists.newArrayList(stop_a, stop_c));

    // A to D
    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, graph, stop_a, stop_d))
        .getShortestPathTree();

    path = spt.getPath(stop_d);
    assertNotNull(path);
    assertEquals(extractStopVertices(path), Lists.newArrayList(stop_a, stop_c, stop_d));
    long endTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 0, 0, 0) + 40 * 60;
    assertEquals(endTime, path.getEndTime());

    // A to E
    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, graph, stop_a, stop_e))
        .getShortestPathTree();

    path = spt.getPath(stop_e);
    assertNotNull(path);
    assertEquals(extractStopVertices(path), Lists.newArrayList(stop_a, stop_c, stop_e));
    endTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 0, 0, 0) + 70 * 60;
    assertEquals(endTime, path.getEndTime());
  }

  private List<Vertex> extractStopVertices(GraphPath path) {
    List<Vertex> ret = Lists.newArrayList();
    for (State state : path.states) {
      if (state.getVertex() instanceof TransitStopVertex) {
        ret.add(state.getVertex());
      }
    }
    return ret;
  }
}
