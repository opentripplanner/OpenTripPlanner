package org.opentripplanner.routing.edgetype.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

import com.google.common.collect.Iterables;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.graph_builder.issues.NegativeHopTime;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

/**
 * TODO OTP2 - Test is too close to the implementation and will need to be reimplemented.
 */
@Disabled
public class GeometryProcessorTest {

  private Graph graph;
  private String feedId;
  private DefaultDataImportIssueStore issueStore;

  @BeforeEach
  public void setUp() throws Exception {
    var deduplicator = new Deduplicator();
    var stopModel = new StopModel();
    graph = new Graph(deduplicator);
    TransitModel transitModel = new TransitModel(stopModel, deduplicator);
    this.issueStore = new DefaultDataImportIssueStore();

    GtfsContext context = contextBuilder(ConstantsForTests.FAKE_GTFS)
      .withIssueStoreAndDeduplicator(graph)
      .build();

    feedId = context.getFeedId().getId();
    transitModel.updateCalendarServiceData(true, context.getCalendarServiceData(), null);

    String[] stops = {
      feedId + ":A",
      feedId + ":B",
      feedId + ":C",
      feedId + ":D",
      feedId + ":E",
      feedId + ":entrance_a",
      feedId + ":entrance_b",
    };
    for (String s : stops) {
      TransitStopVertex stop = (TransitStopVertex) (graph.getVertex(s));

      IntersectionVertex front = new IntersectionVertex(
        graph,
        "near_1_" + stop.getStop().getId(),
        stop.getX() + 0.0001,
        stop.getY() + 0.0001
      );
      IntersectionVertex back = new IntersectionVertex(
        graph,
        "near_2_" + stop.getStop().getId(),
        stop.getX() - 0.0001,
        stop.getY() - 0.0001
      );

      StreetEdge street1 = new StreetEdge(
        front,
        back,
        GeometryUtils.makeLineString(
          stop.getX() + 0.0001,
          stop.getY() + 0.0001,
          stop.getX() - 0.0001,
          stop.getY() - 0.0001
        ),
        "street",
        100,
        StreetTraversalPermission.ALL,
        false
      );
      StreetEdge street2 = new StreetEdge(
        back,
        front,
        GeometryUtils.makeLineString(
          stop.getX() - 0.0001,
          stop.getY() - 0.0001,
          stop.getX() + 0.0001,
          stop.getY() + 0.0001
        ),
        "street",
        100,
        StreetTraversalPermission.ALL,
        true
      );
    }

    //Linkers aren't run otherwise
    graph.hasStreets = true;
    StreetLinkerModule.linkStreetsForTestOnly(graph, transitModel);
  }

  @Test
  public void testIssue() {
    boolean found = false;
    for (DataImportIssue it : issueStore.listIssues()) {
      if (it instanceof NegativeHopTime nht) {
        assertTrue(nht.st0.getDepartureTime() > nht.st1.getArrivalTime());
        found = true;
      }
    }
    assertTrue(found);
  }

  @Test
  public void testRoutingOverMidnight() {
    // this route only runs on weekdays
    Vertex stop_g = graph.getVertex(feedId + ":G_depart");
    Vertex stop_h = graph.getVertex(feedId + ":H_arrive");

    ShortestPathTree<State, Edge, Vertex> spt;
    GraphPath<State, Edge, Vertex> path;
    RouteRequest options = new RouteRequest();

    // Friday evening
    options.setDateTime(
      LocalDateTime.of(2009, Month.AUGUST, 18, 23, 20).atZone(ZoneIds.NEW_YORK).toInstant()
    );

    spt =
      StreetSearchBuilder
        .of()
        .setHeuristic(new EuclideanRemainingWeightHeuristic())
        .setRequest(options)
        .setFrom(stop_g)
        .setTo(stop_h)
        .getShortestPathTree();

    path = spt.getPath(stop_h);
    assertNotNull(path);
    assertEquals(4, path.states.size());

    // Saturday morning
    long startTime = LocalDateTime
      .of(2009, Month.SEPTEMBER, 19, 0, 5)
      .atZone(ZoneIds.NEW_YORK)
      .toEpochSecond();
    options.setDateTime(Instant.ofEpochSecond(startTime));
    spt =
      StreetSearchBuilder
        .of()
        .setHeuristic(new EuclideanRemainingWeightHeuristic())
        .setRequest(options)
        .setFrom(stop_g)
        .setTo(stop_h)
        .getShortestPathTree();

    path = spt.getPath(stop_h);
    assertNotNull(path);
    assertEquals(4, path.states.size());
    long endTime = path.getEndTime();
    assertTrue(endTime < startTime + 60 * 60);
  }

  @Test
  public void testPickupDropoff() {
    Vertex stop_o = graph.getVertex(feedId + ":O_depart");
    Vertex stop_p = graph.getVertex(feedId + ":P");
    assertEquals(2, stop_o.getOutgoing().size());

    RouteRequest options = new RouteRequest();
    options.setDateTime(
      LocalDateTime.of(2009, Month.AUGUST, 19, 12, 0).atZone(ZoneIds.NEW_YORK).toInstant()
    );
    ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder
      .of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(options)
      .setFrom(stop_o)
      .setTo(stop_p)
      .getShortestPathTree();
    GraphPath<State, Edge, Vertex> path = spt.getPath(stop_p);
    assertNotNull(path);
    long endTime = LocalDateTime
      .of(2009, Month.SEPTEMBER, 19, 12, 10)
      .atZone(ZoneIds.NEW_YORK)
      .toEpochSecond();
    assertEquals(endTime, path.getEndTime());

    options.setDateTime(
      LocalDateTime.of(2009, 8, 19, 12, 0, 1).atZone(ZoneIds.NEW_YORK).toInstant()
    );
    spt =
      StreetSearchBuilder
        .of()
        .setHeuristic(new EuclideanRemainingWeightHeuristic())
        .setRequest(options)
        .setFrom(stop_o)
        .setTo(stop_p)
        .getShortestPathTree();
    path = spt.getPath(stop_p);
    assertNotNull(path);
    endTime =
      LocalDateTime.of(2009, Month.SEPTEMBER, 19, 15, 10).atZone(ZoneIds.NEW_YORK).toEpochSecond();
    assertEquals(endTime, path.getEndTime());
  }

  @Test
  public void testTimelessStops() {
    Vertex stop_d = graph.getVertex(feedId + ":D");
    Vertex stop_c = graph.getVertex(feedId + ":C");
    RouteRequest options = new RouteRequest();
    options.setDateTime(
      LocalDateTime.of(2009, Month.AUGUST, 1, 10, 0).atZone(ZoneIds.NEW_YORK).toInstant()
    );
    ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder
      .of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(options)
      .setFrom(stop_d)
      .setTo(stop_c)
      .getShortestPathTree();

    GraphPath<State, Edge, Vertex> path = spt.getPath(stop_c);
    assertNotNull(path);
    assertEquals(
      LocalDateTime.of(2009, Month.SEPTEMBER, 1, 11, 0).atZone(ZoneIds.NEW_YORK).toEpochSecond(),
      path.getEndTime()
    );
  }

  @Test
  public void testWheelchairAccessible() {
    Vertex near_a = graph.getVertex("near_1_" + feedId + "_entrance_a");
    Vertex near_b = graph.getVertex("near_1_" + feedId + "_entrance_b");
    Vertex near_c = graph.getVertex("near_1_" + feedId + "_C");
    Vertex near_e = graph.getVertex("near_1_" + feedId + "_E");

    Vertex stop_d = graph.getVertex(feedId + ":D");
    Vertex split_d = null;
    for (StreetTransitStopLink e : Iterables.filter(
      stop_d.getOutgoing(),
      StreetTransitStopLink.class
    )) {
      split_d = e.getToVertex();
    }

    RouteRequest options = new RouteRequest();
    options.setWheelchair(true);
    options.setDateTime(
      LocalDateTime.of(2009, Month.AUGUST, 18, 0, 0).atZone(ZoneIds.NEW_YORK).toInstant()
    );

    ShortestPathTree<State, Edge, Vertex> spt;
    GraphPath<State, Edge, Vertex> path;

    // stop B is accessible, so there should be a path.
    spt =
      StreetSearchBuilder
        .of()
        .setHeuristic(new EuclideanRemainingWeightHeuristic())
        .setRequest(options)
        .setFrom(near_a)
        .setTo(near_b)
        .getShortestPathTree();

    path = spt.getPath(near_b);
    assertNotNull(path);

    // stop C is not accessible, so there should be no path.
    spt =
      StreetSearchBuilder
        .of()
        .setHeuristic(new EuclideanRemainingWeightHeuristic())
        .setRequest(options)
        .setFrom(near_a)
        .setTo(near_c)
        .getShortestPathTree();

    path = spt.getPath(near_c);
    assertNull(path);

    // stop E has no accessibility information, but we should still be able to route to it.
    spt =
      StreetSearchBuilder
        .of()
        .setHeuristic(new EuclideanRemainingWeightHeuristic())
        .setRequest(options)
        .setFrom(near_a)
        .setTo(near_e)
        .getShortestPathTree();

    path = spt.getPath(near_e);
    assertNotNull(path);

    // from stop A to stop D would normally be trip 1.1 to trip 2.1, arriving at 00:30. But trip
    // 2 is not accessible, so we'll do 1.1 to 3.1, arriving at 01:00
    LocalDateTime ldt = LocalDateTime.of(2009, 7, 18, 0, 0, 0);
    ZonedDateTime zdt = ZonedDateTime.of(ldt, ZoneIds.NEW_YORK);
    options.setDateTime(zdt.toInstant());
    spt =
      StreetSearchBuilder
        .of()
        .setHeuristic(new EuclideanRemainingWeightHeuristic())
        .setRequest(options)
        .setFrom(near_a)
        .setTo(split_d)
        .getShortestPathTree();

    ZonedDateTime endTime = zdt.plusHours(1).plusSeconds(1);
    // 1 sec for the StreetTransitLink
    path = spt.getPath(split_d);
    assertNotNull(path);
    assertEquals(endTime.toEpochSecond(), path.getEndTime());
  }

  @Test
  public void testRunForTrain() {
    // This is the notorious Byrd bug: we're going from Q to T at 8:30.
    // There's a trip from S to T at 8:50 and a second one at 9:50.
    // To get to S by 8:50, we need to take trip 12.1 from Q to R, and 13.1
    // from R to S.  If we take the direct-but-slower 11.1, we'll miss
    // the 8:50 and have to catch the 9:50.
    Vertex destination = graph.getVertex(feedId + ":T");
    RouteRequest options = new RouteRequest();
    // test is designed such that transfers must be instantaneous
    options.withPreferences(pref -> pref.withTransfer(tx -> tx.withSlack(0)));
    LocalDateTime ldt = LocalDateTime.of(2009, 10, 2, 8, 30);
    ZonedDateTime startTime = ZonedDateTime.of(ldt, ZoneIds.NEW_YORK);
    options.setDateTime(startTime.toInstant());
    ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder
      .of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(options)
      .setFrom(graph.getVertex(feedId + ":Q"))
      .setTo(destination)
      .getShortestPathTree();
    GraphPath<State, Edge, Vertex> path = spt.getPath(destination);

    long endTime = path.getEndTime();
    assertTrue(endTime - startTime.toEpochSecond() < 7200);
  }

  @Test
  public void testFrequencies() {
    Vertex stop_u = graph.getVertex(feedId + ":U_depart");
    Vertex stop_v = graph.getVertex(feedId + ":V_arrive");

    ShortestPathTree<State, Edge, Vertex> spt;
    GraphPath<State, Edge, Vertex> path;

    RouteRequest options = new RouteRequest();
    options.setDateTime(
      LocalDateTime.of(2009, Month.AUGUST, 7, 0, 0).atZone(ZoneIds.NEW_YORK).toInstant()
    );

    // U to V - original stop times - shouldn't be used
    spt =
      StreetSearchBuilder
        .of()
        .setHeuristic(new EuclideanRemainingWeightHeuristic())
        .setRequest(options)
        .setFrom(stop_u)
        .setTo(stop_v)
        .getShortestPathTree();
    path = spt.getPath(stop_v);
    assertNotNull(path);
    assertEquals(4, path.states.size());
    long endTime = LocalDateTime.of(2009, 8 + 1, 7, 6, 40).atZone(ZoneIds.NEW_YORK).toEpochSecond();
    assertEquals(endTime, path.getEndTime());

    // U to V - first frequency
    options.setDateTime(
      LocalDateTime.of(2009, Month.AUGUST, 7, 7, 0).atZone(ZoneIds.NEW_YORK).toInstant()
    );
    spt =
      StreetSearchBuilder
        .of()
        .setHeuristic(new EuclideanRemainingWeightHeuristic())
        .setRequest(options)
        .setFrom(stop_u)
        .setTo(stop_v)
        .getShortestPathTree();
    path = spt.getPath(stop_v);
    assertNotNull(path);
    assertEquals(4, path.states.size());
    endTime =
      LocalDateTime.of(2009, Month.SEPTEMBER, 7, 7, 40).atZone(ZoneIds.NEW_YORK).toEpochSecond();
    assertEquals(endTime, path.getEndTime());

    // U to V - second frequency
    options.setDateTime(
      LocalDateTime.of(2009, Month.AUGUST, 7, 14, 0).atZone(ZoneIds.NEW_YORK).toInstant()
    );
    spt =
      StreetSearchBuilder
        .of()
        .setHeuristic(new EuclideanRemainingWeightHeuristic())
        .setRequest(options)
        .setFrom(stop_u)
        .setTo(stop_v)
        .getShortestPathTree();
    path = spt.getPath(stop_v);
    assertNotNull(path);
    assertEquals(4, path.states.size());
    endTime =
      LocalDateTime.of(2009, Month.SEPTEMBER, 7, 14, 40).atZone(ZoneIds.NEW_YORK).toEpochSecond();
    assertEquals(endTime, path.getEndTime());
    // TODO more detailed testing of frequencies

  }

  @Test
  public void testPathways() {
    Vertex entrance = graph.getVertex(feedId + ":entrance_a");
    assertNotNull(entrance);
    Vertex stop = graph.getVertex(feedId + ":A");
    assertNotNull(stop);

    RouteRequest options = new RouteRequest();
    options.setDateTime(
      LocalDateTime.of(2009, Month.AUGUST, 1, 16, 0).atZone(ZoneIds.NEW_YORK).toInstant()
    );
    ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder
      .of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(options)
      .setFrom(entrance)
      .setTo(stop)
      .getShortestPathTree();

    GraphPath<State, Edge, Vertex> path = spt.getPath(stop);
    assertNotNull(path);
    assertEquals(
      LocalDateTime
        .of(2009, Month.SEPTEMBER, 1, 16, 0, 34)
        .atZone(ZoneIds.NEW_YORK)
        .toEpochSecond(),
      path.getEndTime()
    );
  }
}
