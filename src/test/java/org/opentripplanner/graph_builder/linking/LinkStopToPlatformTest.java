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
   * Tests that extra edges are added when linking stops to platform areas to prevent detours around
   */
  @Test
  public void testLinkStopOutsideArea() {
    // test platform is a simple rectangle. It creates a graph of 8 edges.
    Coordinate platform[] = {
      new Coordinate(60.0001, 10),
      new Coordinate(60.0001, 10.0002),
      new Coordinate(60, 10.0002),
      new Coordinate(60, 10),
    };
    // add entrance to every corner of the platform (this array defines indices)
    int visibilityPoints[] = { 0, 1, 2, 3 };

    // place one stop outside the platform, halway under the bottom edge
    Coordinate stop = new Coordinate(59.9999, 10.0001);

    Graph graph = prepareTest(platform, visibilityPoints, stop);
    linkStops(graph);

    // bottom edge gets split into half and split point connects to the stop. 4 new eges get added.
    assertEquals(12, graph.getEdges().size());
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

    LOG.info("edgelen {}", line.getLength());
    return new AreaEdge(
      v1,
      v2,
      line,
      name,
      line.getLength(),
      StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
      false,
      area
    );
  }
}
