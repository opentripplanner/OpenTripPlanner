package org.opentripplanner.routing.edgetype.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

import com.google.common.collect.Iterables;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.NegativeHopTime;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.geometry.GeometryProcessor;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.routing.algorithm.astar.AStarBuilder;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitStopLink;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.util.TestUtils;

/**
 * TODO OTP2 - Test is too close to the implementation and will need to be reimplemented.
 */
@Disabled
public class GeometryProcessorTest {

  private Graph graph;
  private TransitModel transitModel;

  private GtfsContext context;
  private String feedId;
  private DataImportIssueStore issueStore;

  @BeforeEach
  public void setUp() throws Exception {
    var deduplicator = new Deduplicator();
    var stopModel = new StopModel();
    graph = new Graph(stopModel, deduplicator);
    transitModel = new TransitModel(stopModel, deduplicator);
    this.issueStore = new DataImportIssueStore(true);

    context =
      contextBuilder(ConstantsForTests.FAKE_GTFS).withIssueStoreAndDeduplicator(graph).build();

    feedId = context.getFeedId().getId();
    GeometryProcessor factory = new GeometryProcessor(context);
    factory.run(transitModel);
    transitModel.putService(CalendarServiceData.class, context.getCalendarServiceData());

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

    StreetLinkerModule ttsnm = new StreetLinkerModule();
    //Linkers aren't run otherwise
    graph.hasStreets = true;
    transitModel.setHasTransit(true);
    ttsnm.buildGraph(graph, transitModel, new HashMap<>());
  }

  @Test
  public void testIssue() {
    boolean found = false;
    for (DataImportIssue it : issueStore.getIssues()) {
      if (it instanceof NegativeHopTime) {
        NegativeHopTime nht = (NegativeHopTime) it;
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

    ShortestPathTree spt;
    GraphPath path;
    RoutingRequest options = new RoutingRequest();

    // Friday evening
    options.setDateTime(TestUtils.dateInstant("America/New_York", 2009, 8, 18, 23, 20, 0));

    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, graph, stop_g, stop_h))
        .getShortestPathTree();

    path = spt.getPath(stop_h);
    assertNotNull(path);
    assertEquals(4, path.states.size());

    // Saturday morning
    long startTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 19, 0, 5, 0);
    options.setDateTime(Instant.ofEpochSecond(startTime));
    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, graph, stop_g, stop_h))
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

    RoutingRequest options = new RoutingRequest();
    options.setDateTime(TestUtils.dateInstant("America/New_York", 2009, 8, 19, 12, 0, 0));
    ShortestPathTree spt = AStarBuilder
      .oneToOne()
      .setContext(new RoutingContext(options, graph, stop_o, stop_p))
      .getShortestPathTree();
    GraphPath path = spt.getPath(stop_p);
    assertNotNull(path);
    long endTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 19, 12, 10, 0);
    assertEquals(endTime, path.getEndTime());

    options.setDateTime(TestUtils.dateInstant("America/New_York", 2009, 8, 19, 12, 0, 1));
    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, graph, stop_o, stop_p))
        .getShortestPathTree();
    path = spt.getPath(stop_p);
    assertNotNull(path);
    endTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 19, 15, 10, 0);
    assertEquals(endTime, path.getEndTime());
  }

  @Test
  public void testTimelessStops() {
    Vertex stop_d = graph.getVertex(feedId + ":D");
    Vertex stop_c = graph.getVertex(feedId + ":C");
    RoutingRequest options = new RoutingRequest();
    options.setDateTime(TestUtils.dateInstant("America/New_York", 2009, 8, 1, 10, 0, 0));
    ShortestPathTree spt = AStarBuilder
      .oneToOne()
      .setContext(new RoutingContext(options, graph, stop_d, stop_c))
      .getShortestPathTree();

    GraphPath path = spt.getPath(stop_c);
    assertNotNull(path);
    assertEquals(
      TestUtils.dateInSeconds("America/New_York", 2009, 8, 1, 11, 0, 0),
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

    RoutingRequest options = new RoutingRequest();
    options.wheelchairAccessibility = WheelchairAccessibilityRequest.makeDefault(true);
    options.setDateTime(TestUtils.dateInstant("America/New_York", 2009, 8, 18, 0, 0, 0));

    ShortestPathTree spt;
    GraphPath path;

    // stop B is accessible, so there should be a path.
    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, graph, near_a, near_b))
        .getShortestPathTree();

    path = spt.getPath(near_b);
    assertNotNull(path);

    // stop C is not accessible, so there should be no path.
    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, graph, near_a, near_c))
        .getShortestPathTree();

    path = spt.getPath(near_c);
    assertNull(path);

    // stop E has no accessibility information, but we should still be able to route to it.
    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, graph, near_a, near_e))
        .getShortestPathTree();

    path = spt.getPath(near_e);
    assertNotNull(path);

    // from stop A to stop D would normally be trip 1.1 to trip 2.1, arriving at 00:30. But trip
    // 2 is not accessible, so we'll do 1.1 to 3.1, arriving at 01:00
    LocalDateTime ldt = LocalDateTime.of(2009, 7, 18, 0, 0, 0);
    ZonedDateTime zdt = ZonedDateTime.of(ldt, ZoneId.of("America/New_York"));
    options.setDateTime(zdt.toInstant());
    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, graph, near_a, split_d))
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
    RoutingRequest options = new RoutingRequest();
    // test is designed such that transfers must be instantaneous
    options.transferSlack = 0;
    LocalDateTime ldt = LocalDateTime.of(2009, 10, 2, 8, 30, 0);
    ZonedDateTime startTime = ZonedDateTime.of(ldt, ZoneId.of("America/New_York"));
    options.setDateTime(startTime.toInstant());
    Vertex q = graph.getVertex(feedId + ":Q");
    ShortestPathTree spt = AStarBuilder
      .oneToOne()
      .setContext(new RoutingContext(options, graph, q, destination))
      .getShortestPathTree();
    GraphPath path = spt.getPath(destination);

    long endTime = path.getEndTime();
    assertTrue(endTime - startTime.toEpochSecond() < 7200);
  }

  @Test
  public void testFrequencies() {
    Vertex stop_u = graph.getVertex(feedId + ":U_depart");
    Vertex stop_v = graph.getVertex(feedId + ":V_arrive");

    ShortestPathTree spt;
    GraphPath path;

    RoutingRequest options = new RoutingRequest();
    options.setStreetSubRequestModes(new TraverseModeSet(TraverseMode.TRANSIT));
    options.setDateTime(TestUtils.dateInstant("America/New_York", 2009, 8, 7, 0, 0, 0));

    // U to V - original stop times - shouldn't be used
    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, graph, stop_u, stop_v))
        .getShortestPathTree();
    path = spt.getPath(stop_v);
    assertNotNull(path);
    assertEquals(4, path.states.size());
    long endTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 6, 40, 0);
    assertEquals(endTime, path.getEndTime());

    // U to V - first frequency
    options.setDateTime(TestUtils.dateInstant("America/New_York", 2009, 8, 7, 7, 0, 0));
    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, graph, stop_u, stop_v))
        .getShortestPathTree();
    path = spt.getPath(stop_v);
    assertNotNull(path);
    assertEquals(4, path.states.size());
    endTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 7, 40, 0);
    assertEquals(endTime, path.getEndTime());

    // U to V - second frequency
    options.setDateTime(TestUtils.dateInstant("America/New_York", 2009, 8, 7, 14, 0, 0));
    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, graph, stop_u, stop_v))
        .getShortestPathTree();
    path = spt.getPath(stop_v);
    assertNotNull(path);
    assertEquals(4, path.states.size());
    endTime = TestUtils.dateInSeconds("America/New_York", 2009, 8, 7, 14, 40, 0);
    assertEquals(endTime, path.getEndTime());
    // TODO more detailed testing of frequencies

  }

  @Test
  public void testPathways() {
    Vertex entrance = graph.getVertex(feedId + ":entrance_a");
    assertNotNull(entrance);
    Vertex stop = graph.getVertex(feedId + ":A");
    assertNotNull(stop);

    RoutingRequest options = new RoutingRequest();
    options.setDateTime(TestUtils.dateInstant("America/New_York", 2009, 8, 1, 16, 0, 0));
    ShortestPathTree spt = AStarBuilder
      .oneToOne()
      .setContext(new RoutingContext(options, graph, entrance, stop))
      .getShortestPathTree();

    GraphPath path = spt.getPath(stop);
    assertNotNull(path);
    assertEquals(
      TestUtils.dateInSeconds("America/New_York", 2009, 8, 1, 16, 0, 34),
      path.getEndTime()
    );
  }
}
