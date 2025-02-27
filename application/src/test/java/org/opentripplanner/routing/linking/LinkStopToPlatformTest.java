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
import org.opentripplanner.street.model.vertex.LabelledIntersectionVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
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
    var timetableRepository = new TimetableRepository(siteRepository, deduplicator);
    ArrayList<IntersectionVertex> vertices = new ArrayList<>();
    Coordinate[] closedGeom = new Coordinate[platform.length + 1];

    for (int i = 0; i < platform.length; i++) {
      Coordinate c = platform[i];
      var vertex = new LabelledIntersectionVertex(String.valueOf(i), c.x, c.y, false, false);
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

    // AreaGroup must include a valid Area which defines area atttributes
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
    graph.index(timetableRepository.getSiteRepository());

    for (RegularStop s : transitStops) {
      var v = TransitStopVertex.of().withStop(s).build();
      graph.addVertex(v);
    }

    return graph;
  }

  /**
   * Link stop outside platform area to platform.
   */
  @Test
  public void testLinkStopOutsideArea() {
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
    linkStops(graph);

    // Two bottom edges gets split into half (+2 edges)
    // both split points are linked to the stop bidirectonally (+4 edges).
    // both split points also link to 2 visibility points at opposite side (+8 edges)
    // 14 new edges in total
    assertEquals(22, graph.getEdges().size());
  }

  /**
   * Link stop inside platform area to platform. Connects stop with visibility points.
   */
  @Test
  public void testLinkStopInsideArea() {
    // test platform is a simple rectangle. It creates a graph of 8 edges.
    Coordinate[] platform = {
      new Coordinate(10, 60.002),
      new Coordinate(10.004, 60.002),
      new Coordinate(10.004, 60),
      new Coordinate(10, 60),
    };
    // add entrance to every corner of the platform (this array defines indices)
    int[] visibilityPoints = { 0, 1, 2, 3 };

    // place one stop inside the platform, near bottom left corner
    Coordinate[] stops = { new Coordinate(10.001, 60.001) };

    Graph graph = prepareTest(platform, visibilityPoints, stops);
    linkStops(graph);

    // stop links to a new street vertex with 2 edges
    // new vertex connects to all 4 visibility points with 4*2 new edges
    assertEquals(18, graph.getEdges().size());

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
  public void testLinkStopNearPlatformVertex() {
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
    linkStops(graph);

    // stop links to a existing vertex with 2 edges
    assertEquals(10, graph.getEdges().size());
  }

  /**
   * Link two stops inside platform area to platform.
   * Stops will get linked directly.
   */
  @Test
  public void testLinkTwoStopsInsideArea() {
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
    linkStops(graph);

    // stops are linked with 2 new street vertices with 2 edges each (+4)
    // new vertices connects to original 4 visibility points with 2*4*2 new edges (+16)
    // stops are also linked directly (+2)
    assertEquals(30, graph.getEdges().size());

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

  private void linkStops(Graph graph) {
    VertexLinker linker = graph.getLinker();

    for (TransitStopVertex tStop : graph.getVerticesOfType(TransitStopVertex.class)) {
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
