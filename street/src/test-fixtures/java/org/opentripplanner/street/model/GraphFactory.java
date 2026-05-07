package org.opentripplanner.street.model;

import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.edge.BoardingLocationToStopLink;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;

public class GraphFactory {
  private final Graph graph = new Graph();
  private int vertexCounter;
  private static final WgsCoordinate origin = new WgsCoordinate(0, 0);

  public GraphFactory() {
    vertexCounter = 0;
  }

  public static GraphFactory of() {
    return new GraphFactory();
  }

  public IntersectionVertex vertex() {
    return intersectionVertex(nextCoordinate());
  }

  public IntersectionVertex intersectionVertex(WgsCoordinate coordinate) {
    var label = "V" + vertexCounter;
    var v = StreetModelFactory.intersectionVertex(label, coordinate);
    graph.addVertex(v);
    vertexCounter++;
    return v;
  }

  public TransitStopVertex stopVertex(FeedScopedId id) {
    return stopVertex(id, nextCoordinate());
  }

  public TransitStopVertex stopVertex(FeedScopedId id, WgsCoordinate coordinate) {
    var v = TransitStopVertex.of()
      .withId(id)
      .withCoordinate(coordinate)
      .build();
    graph.addVertex(v);
    vertexCounter++;
    return v;
  }

  public StreetEdgeBuilder<?> street(StreetVertex v1, StreetVertex v2) {
    var geom = GeometryUtils.makeLineString(v1.toWgsCoordinate(), v2.toWgsCoordinate());
    return new StreetEdgeBuilder<>()
      .withFromVertex(v1)
      .withToVertex(v2)
      .withGeometry(geom)
      .withName("TestEdge")
      .withMeterLength(100)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(false);
  }

  public void link(TransitStopVertex stop, StreetVertex streetVertex) {
    BoardingLocationToStopLink.createBoardingLocationToStopLink(stop, streetVertex);
  }

  public void link(StreetVertex streetVertex, TransitStopVertex stop) {
    BoardingLocationToStopLink.createBoardingLocationToStopLink(streetVertex, stop);
  }

  public Graph buildGraph() {
    graph.hasStreets = true;
    graph.index();
    return graph;
  }

  private WgsCoordinate nextCoordinate() {
    return origin.moveEastMeters(vertexCounter * 10);
  }

}
