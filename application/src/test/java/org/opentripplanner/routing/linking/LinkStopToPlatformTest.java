package org.opentripplanner.routing.linking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Area;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.AreaEdgeBuilder;
import org.opentripplanner.street.model.edge.AreaGroup;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.LinkingDirection;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexFactory;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.test.support.GeoJsonIo;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

public class LinkStopToPlatformTest {

  private static final GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();

  private Graph prepareTest(Coordinate[] platform, int[] visible, Coordinate[] stops) {
    var deduplicator = new Deduplicator();
    var siteRepository = new SiteRepository();
    Graph graph = new Graph(deduplicator);
    var vertexFactory = new VertexFactory(graph);

    var timetableRepository = new TimetableRepository(siteRepository, deduplicator);
    ArrayList<IntersectionVertex> vertices = new ArrayList<>();
    Coordinate[] closedGeom = new Coordinate[platform.length + 1];

    for (int i = 0; i < platform.length; i++) {
      Coordinate c = platform[i];
      var vertex = vertexFactory.intersection(String.valueOf(i), c.x, c.y);
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
    area.setPermission(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
    area.setOriginalEdges(polygon);
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
        "edge " + String.valueOf(i + platform.length)
      );
      // make one corner surrounded by walk nothru edges
      if (i < 2) {
        edgeBuilder1.withWalkNoThruTraffic(true);
        edgeBuilder2.withWalkNoThruTraffic(true);
      }
      edgeBuilder1.buildAndConnect();
      edgeBuilder2.buildAndConnect();
    }

    RegularStop[] transitStops = new RegularStop[stops.length];
    for (int i = 0; i < stops.length; i++) {
      Coordinate stop = stops[i];
      transitStops[i] = testModel.stop("TestStop " + i).withCoordinate(stop.y, stop.x).build();
    }

    timetableRepository.index();
    graph.index();

    for (RegularStop s : transitStops) {
      vertexFactory.transitStop(TransitStopVertex.of().withStop(s));
    }

    return graph;
  }

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

    Graph graph = prepareTest(platform, visibilityPoints, stops);
    linkStops(graph, 100, true);

    // Two bottom edges gets split into half (+2 edges)
    // both split points are linked to the stop bidirectonally (+4 edges).
    // both split points also link to 2 visibility points at opposite side (+8 edges)
    // 14 new edges in total
    assertEquals(22, graph.getEdges().size());
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

    Graph graph = prepareTest(platform, visibilityPoints, stops);
    linkStops(graph, 100, true);

    // stop links to a new street vertex with 2 edges
    // new vertex connects to all 4 visibility points with 4*2 new edges
    // new vertex connects to the closest edge pair split points with 2*2 edges
    // edge pair splits to 2 edges more
    assertEquals(24, graph.getEdges().size());

    // transit stop is connected in one rectangle corner only to walk no thru trafic edges
    // verify that new area edge connection is also walk no thru
    // otherwise connection cannot be used to exit the area
    var noThruEdges = graph
      .getEdgesOfType(AreaEdge.class)
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

    Graph graph = prepareTest(platform, visibilityPoints, stops);
    linkStops(graph, 100, true);

    // stop links to a existing vertex with 2 edges
    assertEquals(10, graph.getEdges().size());
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

    Graph graph = prepareTest(platform, visibilityPoints, stops);

    // dig up the AreaGroup
    AreaGroup ag = null;
    var edge = graph.getEdges().stream().findFirst().get();
    if (edge instanceof AreaEdge ae) {
      ag = ae.getArea();
    }
    assertNotNull(ag);

    var vertexFactory = new VertexFactory(graph);
    var v = vertexFactory.intersection("boardingLocation", 10.00000001, 60.00000001);
    graph.getLinker().addPermanentAreaVertex(v, ag);

    // vertex links to the single visibility point with 2 edges
    assertEquals(10, graph.getEdges().size());
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

    Graph graph = prepareTest(platform, visibilityPoints, stops);
    linkStops(graph, 100, true);

    // Bottom edge pair splits in the middle (+2)
    // Stop links to split vertices (+4)
    // Split vertices link with visibily vertices at top corners (+8)
    assertEquals(22, graph.getEdges().size());
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

    Graph graph = prepareTest(platform, visibilityPoints, stops);
    linkStops(graph, 100, true);

    // stops are linked with 2 new street vertices with 2 edges each (+4)
    // new vertices connect to original 4 visibility points with 2*4*2 new edges (+16)
    // stops are also linked directly (+2)
    // each stop links bidirectionally to closest edge pair (+ 2*2*2)
    // closest edge pairs split into two (+ 2*2)
    assertEquals(42, graph.getEdges().size());

    // verify direct linking
    List<TransitStopVertex> transitStops = graph.getVerticesOfType(TransitStopVertex.class);
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

    Graph graph = prepareTest(platform, visibilityPoints, stops);
    linkStops(graph, 100, true);

    // stop links to the edge pair 2-3, which adds 4 new edges
    // edge pair splitting adds 2 edges, and transit vertex linking 2 more
    // new splitting vertices cannot connect with the visibility points
    // because they are hidden behind the corner
    assertEquals(
      20,
      graph.getEdges().size(),
      "Incorrect number of edges, check %s".formatted(GeoJsonIo.toUrl(graph))
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

    Graph graph = prepareTest(platform, visibilityPoints, stops);
    // set very low visibility node limit for testing purposes
    linkStops(graph, 4, false);

    // stop links to closest edge (+8 edges)
    // it does not link to all 8 visibility points because of low limit
    assertTrue(graph.getEdges().size() < 40);

    // verify that nearest visibility vertex is connected
    var transitStop = graph.getVerticesOfType(TransitStopVertex.class).get(0);
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
    assertTrue(v.isConnected(far) == false);
  }

  private void linkStops(Graph graph, int maxAreaNodes, boolean permanent) {
    VertexLinker linker = graph.getLinker();
    linker.setMaxAreaNodes(maxAreaNodes);
    for (TransitStopVertex tStop : graph.getVerticesOfType(TransitStopVertex.class)) {
      if (permanent) {
        linker.linkVertexPermanently(
          tStop,
          new TraverseModeSet(TraverseMode.WALK),
          LinkingDirection.BIDIRECTIONAL,
          (vertex, streetVertex) ->
            List.of(
              StreetTransitStopLink.createStreetTransitStopLink(
                (TransitStopVertex) vertex,
                streetVertex
              ),
              StreetTransitStopLink.createStreetTransitStopLink(
                streetVertex,
                (TransitStopVertex) vertex
              )
            )
        );
      } else {
        linker.linkVertexForRealTime(
          tStop,
          new TraverseModeSet(TraverseMode.WALK),
          LinkingDirection.BIDIRECTIONAL,
          (vertex, streetVertex) ->
            List.of(
              StreetTransitStopLink.createStreetTransitStopLink(
                (TransitStopVertex) vertex,
                streetVertex
              ),
              StreetTransitStopLink.createStreetTransitStopLink(
                streetVertex,
                (TransitStopVertex) vertex
              )
            )
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
    LineString line = geometryFactory.createLineString(
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
