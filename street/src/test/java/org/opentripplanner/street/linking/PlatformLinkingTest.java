package org.opentripplanner.street.linking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.linking.VisibilityMode.COMPUTE_AREA_VISIBILITY_LINES;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.LocalizedString;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.graph.GraphDataFetcher;
import org.opentripplanner.street.model.StreetModelFactory;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Area;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.AreaEdgeBuilder;
import org.opentripplanner.street.model.edge.AreaGroup;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;

public class PlatformLinkingTest {

  private static final GeometryFactory GEOMETRY_FACTORY = GeometryUtils.getGeometryFactory();

  /**
   * Link stop outside platform area to platform.
   * Stop gets linked to the closest edge pair and optimal paths from
   * the splitting points to visibility points are added
   */
  @Test
  void testLinkStopOutsideArea() {
    // test platform is a simple rectangle. It creates a graph of 8 edges.
    Coordinate[] platform = {
      new Coordinate(10, 60.001),
      new Coordinate(10.002, 60.001),
      new Coordinate(10.002, 60),
      new Coordinate(10, 60),
    };
    // add entrance to every corner of the platform (this array defines indices)
    int[] visibilityPoints = { 0, 1, 2, 3 };

    // place one stop outside the platform, halway under the bottom edge
    Coordinate[] stops = { new Coordinate(10.001, 59.9999) };

    var graph = prepareTest(platform, visibilityPoints, stops);
    linkStops(graph, 100, true);

    // Two bottom edges gets split into half (+2 edges)
    // both split points are linked to the stop bidirectionally (+4 edges).
    // both split points also link to 2 visibility points at opposite side (+8 edges)
    // 14 new edges in total
    assertEquals(22, graph.listEdges().size());
  }

  /**
   * Link stop inside platform area to platform.
   * Connects stop with visibility points and to closest edge.
   */
  @Test
  void testLinkStopInsideArea() {
    // Test platform is a simple rectangle. It creates a graph of 8 edges.
    // The stop linking link to edges 100m away (0.000899 degrees), so we make the rectangle smaller
    Coordinate[] platform = {
      new Coordinate(10, 60.0006),
      new Coordinate(10.0008, 60.0006),
      new Coordinate(10.0008, 60),
      new Coordinate(10, 60),
    };
    // add entrance to every corner of the platform (this array defines indices)
    int[] visibilityPoints = { 0, 1, 2, 3 };

    // place one stop inside the platform, near bottom edge mid point
    Coordinate[] stops = { new Coordinate(10.0004, 60.0001) };

    var graph = prepareTest(platform, visibilityPoints, stops);
    linkStops(graph, 100, true);

    // stop links to a new street vertex with 2 edges
    // new vertex connects to all 4 visibility points with 4*2 new edges
    // new vertex connects to the closest edge pair split points with 2*2 edges
    // edge pair splits to 2 edges more
    assertEquals(24, graph.listEdges().size());

    // transit stop is connected in one rectangle corner only to walk no thru trafic edges
    // verify that new area edge connection is also walk no thru
    // otherwise connection cannot be used to exit the area
    var noThruEdges = graph
      .listAreaEdges()
      .stream()
      .filter(a -> a.isWalkNoThruTraffic())
      .toList();
    // original platform has 4 nothru edges, now 2 more got added
    assertEquals(6, noThruEdges.size());
  }

  /**
   * Link stop which is very close to a platform vertex.
   * Linking snaps directly to the vertex.
   * Connections to other vertices are not created.
   */
  @Test
  void testLinkStopNearPlatformVertex() {
    Coordinate[] platform = {
      new Coordinate(10, 60.002),
      new Coordinate(10.004, 60.002),
      new Coordinate(10.004, 60),
      new Coordinate(10, 60),
    };
    // add entrance to every corner of the platform
    int[] visibilityPoints = { 0, 1, 2, 3 };

    // place one stop inside the platform, very near of the bottom left corner
    Coordinate[] stops = { new Coordinate(10.00000001, 60.00000001) };

    var graph = prepareTest(platform, visibilityPoints, stops);
    linkStops(graph, 100, true);

    // stop links to a existing vertex with 2 edges
    assertEquals(10, graph.listEdges().size());
  }

  /**
   * Link an interior vertex which is very close to a visibility vertex by
   * calling directly addPermanentAreaVertex used in boarding location linking
   * A connecting edge pair is created despite of the small distance
   */
  @Test
  void testAddPermanentAreaVertex() {
    Coordinate[] platform = {
      new Coordinate(10, 60.002),
      new Coordinate(10.004, 60.002),
      new Coordinate(10.004, 60),
      new Coordinate(10, 60),
    };
    // add one entrance to bottom left corner
    int[] visibilityPoints = { 3 };

    // No stops
    Coordinate[] stops = {};

    var graph = prepareTest(platform, visibilityPoints, stops);

    // dig up the AreaGroup
    AreaGroup ag = null;
    var edge = graph.listEdges().getFirst();
    if (edge instanceof AreaEdge ae) {
      ag = ae.getArea();
    }
    assertNotNull(ag);

    var v = intersectionVertex("boardingLocation", 10.00000001, 60.00000001);

    var linker = new VertexLinker(
      graph.graph(),
      VisibilityMode.COMPUTE_AREA_VISIBILITY_LINES,
      50,
      true
    );
    linker.addPermanentAreaVertex(v, ag);

    // vertex links to the single visibility point with 2 edges
    assertEquals(10, graph.listEdges().size());

    // check that link edges obey area safety factors
    var out = v.getOutgoing();
    assertEquals(out.size(), 1);
    StreetEdge streetEdge = null;
    if (out.iterator().next() instanceof StreetEdge se) {
      streetEdge = se;
      assertEquals(0.5, se.getWalkSafetyFactor());
      assertEquals(0.5, se.getBicycleSafetyFactor());
    }
    assertNotNull(streetEdge);
  }

  /**
   * Link a stop which is inside an area and very close to its edge.
   * Linking snaps directly to the edge without short connecting edges
   */
  @Test
  void testLinkStopNearPlatformEdge() {
    Coordinate[] platform = {
      new Coordinate(10, 60.002),
      new Coordinate(10.004, 60.002),
      new Coordinate(10.004, 60),
      new Coordinate(10, 60),
    };
    // add entrance to every corner of the platform
    int[] visibilityPoints = { 0, 1, 2, 3 };

    // place one stop inside the platform, very near of the bottom edge
    Coordinate[] stops = { new Coordinate(10.002, 60.00000001) };

    var graph = prepareTest(platform, visibilityPoints, stops);
    linkStops(graph, 100, true);

    // Bottom edge pair splits in the middle (+2)
    // Stop links to split vertices (+4)
    // Split vertices link with visibily vertices at top corners (+8)
    assertEquals(22, graph.listEdges().size());
  }

  /**
   * Link two stops inside platform area to platform.
   * Stops will get linked directly.
   */
  @Test
  void testLinkTwoStopsInsideArea() {
    Coordinate[] platform = {
      new Coordinate(10, 60.002),
      new Coordinate(10.004, 60.002),
      new Coordinate(10.004, 60),
      new Coordinate(10, 60),
    };
    // entrance to every corner of the platform
    int[] visibilityPoints = { 0, 1, 2, 3 };

    // place two stops inside the platform
    Coordinate[] stops = { new Coordinate(10.001, 60.001), new Coordinate(10.003, 60.001) };

    var graph = prepareTest(platform, visibilityPoints, stops);
    linkStops(graph, 100, true);

    // stops are linked with 2 new street vertices with 2 edges each (+4)
    // new vertices connect to original 4 visibility points with 2*4*2 new edges (+16)
    // stops are also linked directly (+2)
    // each stop links bidirectionally to closest edge pair (+ 2*2*2)
    // closest edge pairs split into two (+ 2*2)
    assertEquals(42, graph.listEdges().size());

    // verify direct linking
    List<TransitStopVertex> transitStops = graph.listStopVertices();
    Vertex v1 = null;
    Vertex v2 = null;
    for (Edge e : transitStops.get(0).getOutgoing()) {
      v1 = e.getToVertex();
    }
    for (Edge e : transitStops.get(1).getOutgoing()) {
      v2 = e.getToVertex();
    }
    assertNotNull(v1);
    assertNotNull(v2);
    assertTrue(v1.isConnected(v2));
  }

  /**
   * Link stop inside a concave platform. Stop gets connected to the graph,
   * but visibility edges which would cross the area boundary are not added
   */
  @Test
  void testLinkStopToConcaveArea() {
    /* test platform has a L shape with 12 edges:

      0                    1
       ____________________
      |                    |
      |____________ 4      |
                   |       |
      5            |       |
                   |  *    |
                  3 _______ 2
    */

    Coordinate[] platform = {
      new Coordinate(10, 60.004),
      new Coordinate(10.010, 60.004),
      new Coordinate(10.010, 60),
      new Coordinate(10.006, 60),
      new Coordinate(10.006, 60.003),
      new Coordinate(10, 60.003),
    };
    // add entrances to corners 0 and 5
    int[] visibilityPoints = { 0, 5 };

    // place the stop marked above by * inside the platform, near the edge 2-3
    Coordinate[] stops = { new Coordinate(10.007, 60.001) };

    var graph = prepareTest(platform, visibilityPoints, stops);
    linkStops(graph, 100, true);

    // stop links to the edge pair 2-3, which adds 4 new edges
    // edge pair splitting adds 2 edges, and transit vertex linking 2 more
    // new splitting vertices cannot connect with the visibility points
    // because they are hidden behind the corner
    assertEquals(
      20,
      graph.listEdges().size(),
      "Incorrect number of edges, check %s".formatted(graph.geoJsonUrl())
    );
  }

  /**
   * Test that linker obeys maxAreaNodes linking limit
   */
  @Test
  void testMaxAreaNodes() {
    // oktagonal platform of 16 edges
    Coordinate[] platform = {
      new Coordinate(10, 60.002),
      new Coordinate(10, 60.004),
      new Coordinate(10.002, 60.006),
      new Coordinate(10.004, 60.006),
      new Coordinate(10.006, 60.004),
      new Coordinate(10.006, 60.002),
      new Coordinate(10.004, 60),
      new Coordinate(10.002, 60),
    };
    // add 8 visibility points (max limit applied in linking is 6)
    int[] visibilityPoints = { 0, 1, 2, 3, 4, 5, 6, 7 };

    // place a stop inside, close to the vertical west edge
    Coordinate[] stops = { new Coordinate(10.001, 60.0025) };

    var graph = prepareTest(platform, visibilityPoints, stops);
    // set very low visibility node limit for testing purposes
    linkStops(graph, 4, false);

    // stop links to closest edge (+8 edges)
    // it does not link to all 8 visibility points because of low limit
    assertTrue(graph.listEdges().size() < 40);

    // verify that the nearest visibility vertex is connected
    var transitStop = graph.listStopVertices().getFirst();
    Vertex v = null;
    for (Edge e : transitStop.getOutgoing()) {
      v = e.getToVertex();
    }
    Vertex near = graph.getVertex("0");
    assertNotNull(v);
    assertNotNull(near);
    assertTrue(v.isConnected(near));
    Vertex far = graph.getVertex("4");
    assertNotNull(far);
    assertFalse(v.isConnected(far));
  }

  /**
   * Test that the edge split point connects to other visibility points.
   * This used to occasionally fail due to jts geometry.contains accuracy limitations.
   * The test geometry is taken from Bletchley station platform 6, where
   * the problem was easy to duplicate.
   */
  @Test
  void boundaryTest() {
    Coordinate[] platform = {
      // northwest
      new Coordinate(-0.7360985, 51.9962091),
      // northeast
      new Coordinate(-0.7360355, 51.9962165),
      // east exit
      new Coordinate(-0.7357519, 51.9953057),
      // southeast
      new Coordinate(-0.7356841, 51.9950911),
      // southwest
      new Coordinate(-0.7357458, 51.9950836),
    };

    // 1 visibility point at eastern exit
    int[] visibilityPoints = { 2 };

    // stop at western boundary splits the visibility edge pair
    Coordinate[] stops = { new Coordinate(-0.73577352323178, 51.995172067528664) };

    var graph = prepareTest(platform, visibilityPoints, stops);
    linkStops(graph, 20, true);

    // Edge split points become visibility points
    var aEdges = graph.listAreaEdges();
    assertEquals(3, aEdges.getFirst().getArea().visibilityVertices().size());

    // platform is a loop of 6 points, which adds 5 area edge pairs
    // western boundary splitting adds an edge pair
    // visibility edge connection from split points to exit adds two pairs more
    // Transit stop linking adds 2 pairs more
    assertEquals(
      20,
      graph.listEdges().size(),
      "Incorrect number of edges, check %s".formatted(graph.geoJsonUrl())
    );
  }

  private GraphDataFetcher prepareTest(Coordinate[] platform, int[] visible, Coordinate[] stops) {
    var graph = new Graph();

    ArrayList<IntersectionVertex> vertices = new ArrayList<>();
    Coordinate[] closedGeom = new Coordinate[platform.length + 1];

    for (int i = 0; i < platform.length; i++) {
      Coordinate c = platform[i];
      var vertex = intersectionVertex(String.valueOf(i), c.y, c.x);
      graph.addVertex(vertex);
      vertices.add(vertex);
      closedGeom[i] = c;
    }
    closedGeom[platform.length] = closedGeom[0];

    Polygon polygon = GeometryUtils.getGeometryFactory().createPolygon(closedGeom);
    AreaGroup areaGroup = new AreaGroup(polygon);

    // visibility vertices are platform entrance points and convex corners
    // which should be directly linked with stops
    for (int i : visible) {
      areaGroup.addVisibilityVertices(Set.of(vertices.get(i)));
    }

    // AreaGroup must include a valid Area which defines area attributes
    Area area = new Area();
    area.setName(new LocalizedString("test platform"));
    area.setWalkSafety(0.5f);
    area.setBicycleSafety(0.5f);
    area.setPermission(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
    area.setGeometry(polygon);
    areaGroup.addArea(area);

    for (int i = 0; i < platform.length; i++) {
      int next_i = (i + 1) % platform.length;

      var edgeBuilder1 = createAreaEdge(
        vertices.get(i),
        vertices.get(next_i),
        areaGroup,
        "edge " + i
      );
      var edgeBuilder2 = createAreaEdge(
        vertices.get(next_i),
        vertices.get(i),
        areaGroup,
        "edge " + (i + platform.length)
      );
      // make one corner surrounded by walk nothru edges
      if (i < 2) {
        edgeBuilder1.withWalkNoThruTraffic(true);
        edgeBuilder2.withWalkNoThruTraffic(true);
      }
      edgeBuilder1.buildAndConnect();
      edgeBuilder2.buildAndConnect();
    }

    for (int i = 0; i < stops.length; i++) {
      Coordinate stop = stops[i];
      var stopVertex = StreetModelFactory.transitStopVertex(i, stop);
      graph.addVertex(stopVertex);
    }

    graph.index();

    return new GraphDataFetcher(graph);
  }

  private void linkStops(GraphDataFetcher graph, int maxAreaNodes, boolean permanent) {
    var linker = new VertexLinker(
      graph.graph(),
      COMPUTE_AREA_VISIBILITY_LINES,
      maxAreaNodes,
      false
    );
    for (TransitStopVertex tStop : graph.listStopVertices()) {
      if (permanent) {
        linker.linkVertexBidirectionallyPermanently(
          tStop,
          new TraverseModeSet(TraverseMode.WALK),
          StreetTransitStopLink::createStreetTransitStopLink
        );
      } else {
        linker.linkVertexBidirectionallyForRealTime(
          tStop,
          new TraverseModeSet(TraverseMode.WALK),
          StreetTransitStopLink::createStreetTransitStopLink
        );
      }
    }
  }

  private AreaEdgeBuilder createAreaEdge(
    IntersectionVertex v1,
    IntersectionVertex v2,
    AreaGroup area,
    String nameString
  ) {
    LineString line = GEOMETRY_FACTORY.createLineString(
      new Coordinate[] { v1.getCoordinate(), v2.getCoordinate() }
    );
    I18NString name = new LocalizedString(nameString);
    return new AreaEdgeBuilder()
      .withFromVertex(v1)
      .withToVertex(v2)
      .withGeometry(line)
      .withName(name)
      .withPermission(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE)
      .withBack(false)
      .withArea(area);
  }
}
