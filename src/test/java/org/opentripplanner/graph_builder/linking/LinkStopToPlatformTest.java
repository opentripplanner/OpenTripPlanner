package org.opentripplanner.graph_builder.linking;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.LinkingDirection;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.AreaEdgeList;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertexBuilder;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkStopToPlatformTest {

  private static final Logger LOG = LoggerFactory.getLogger(LinkStopToPlatformTest.class);

  private static final GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();

  private Graph prepareTest(Coordinate[] platform, int[] visible, Coordinate stop) {
    var deduplicator = new Deduplicator();
    var stopModel = new StopModel();
    Graph graph = new Graph(deduplicator);
    var transitModel = new TransitModel(stopModel, deduplicator);
    ArrayList<IntersectionVertex> vertices = new ArrayList<>();
    Coordinate[] closedGeom = new Coordinate[platform.length + 1];

    for (int i = 0; i < platform.length; i++) {
      Coordinate c = platform[i];
      vertices.add(
        new IntersectionVertex(graph, String.valueOf(i), c.x, c.y, "Platform vertex " + i)
      );
      closedGeom[i] = c;
    }
    closedGeom[platform.length] = closedGeom[0];

    AreaEdgeList areaEdgeList = new AreaEdgeList(
      GeometryUtils.getGeometryFactory().createPolygon(closedGeom),
      Set.of()
    );

    // visibility vertices are platform entrance points and convex corners
    // which should be directly linked with stops
    for (int i : visible) {
      areaEdgeList.visibilityVertices.add(vertices.get(i));
    }

    ArrayList<AreaEdge> edges = new ArrayList<>();

    for (int i = 0; i < platform.length; i++) {
      int next_i = (i + 1) % platform.length;
      edges.add(createAreaEdge(vertices.get(i), vertices.get(next_i), areaEdgeList, "edge " + i));
      edges.add(
        createAreaEdge(
          vertices.get(next_i),
          vertices.get(i),
          areaEdgeList,
          "edge " + String.valueOf(i + platform.length)
        )
      );
    }

    RegularStop transitStop = TransitModelForTest
      .stop("TestStop")
      .withCoordinate(stop.y, stop.x)
      .build();

    transitModel.index();
    graph.index(transitModel.getStopModel());

    new TransitStopVertexBuilder().withGraph(graph).withStop(transitStop).build();

    LOG.info("Test graph size {}", graph.getEdges().size());
    return graph;
  }

  /**
   * Link stop outside platform area to platform. Adds direct connection to closest edges.
   */
  @Test
  public void testLinkStopOutsideArea() {
    // test platform is a simple rectangle. It creates a graph of 8 edges.
    Coordinate platform[] = {
      new Coordinate(10, 60.001),
      new Coordinate(10.002, 60.001),
      new Coordinate(10.002, 60),
      new Coordinate(10, 60),
    };
    // add entrance to every corner of the platform (this array defines indices)
    int visibilityPoints[] = { 0, 1, 2, 3 };

    // place one stop outside the platform, halway under the bottom edge
    Coordinate stop = new Coordinate(10.001, 59.9999);

    Graph graph = prepareTest(platform, visibilityPoints, stop);
    linkStops(graph);

    // Two bottom edges gets split into half and both split points
    // connected to the stop bidirectonally. 6 new eges get added.
    assertEquals(14, graph.getEdges().size());
  }

  /**
   * Link stop inside platform area to platform. Connects stop with visibility points.
   */
  @Test
  public void testLinkStopInsideArea() {
    // test platform is a simple rectangle. It creates a graph of 8 edges.
    Coordinate platform[] = {
      new Coordinate(10, 60.002),
      new Coordinate(10.004, 60.002),
      new Coordinate(10.004, 60),
      new Coordinate(10, 60),
    };
    // add entrance to every corner of the platform (this array defines indices)
    int visibilityPoints[] = { 0, 1, 2, 3 };

    // place one stop inside the platform, near bottom left corner
    Coordinate stop = new Coordinate(10.001, 60.001);

    Graph graph = prepareTest(platform, visibilityPoints, stop);
    linkStops(graph);

    for (Edge e : graph.getEdges()) {
      LOG.info("Edge {}", e);
    }
    // stop connects to all 4 visibility points and 4*2 new edges will be added
    assertEquals(16, graph.getEdges().size());
  }

  private void linkStops(Graph graph) {
    VertexLinker linker = graph.getLinker();

    for (TransitStopVertex tStop : graph.getVerticesOfType(TransitStopVertex.class)) {
      linker.linkVertexPermanently(
        tStop,
        new TraverseModeSet(TraverseMode.WALK),
        LinkingDirection.BOTH_WAYS,
        (vertex, streetVertex) ->
          List.of(
            new StreetTransitStopLink((TransitStopVertex) vertex, streetVertex),
            new StreetTransitStopLink(streetVertex, (TransitStopVertex) vertex)
          )
      );
    }
  }

  private AreaEdge createAreaEdge(
    IntersectionVertex v1,
    IntersectionVertex v2,
    AreaEdgeList area,
    String nameString
  ) {
    LineString line = geometryFactory.createLineString(
      new Coordinate[] { v1.getCoordinate(), v2.getCoordinate() }
    );
    I18NString name = new LocalizedString(nameString);
    return new AreaEdge(
      v1,
      v2,
      line,
      name,
      StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
      false,
      area
    );
  }
}
