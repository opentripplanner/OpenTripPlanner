package org.opentripplanner.routing.vehicle_parking;

import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

public class VehicleParkingTestGraphData {

  protected IntersectionVertex A, B;

  protected Graph graph;

  protected TransitModel transitModel;

  public void initGraph() {
    var deduplicator = new Deduplicator();
    var stopModel = new StopModel();
    graph = new Graph(stopModel, deduplicator);
    transitModel = new TransitModel(stopModel, deduplicator);
    graph.hasStreets = true;

    A = new IntersectionVertex(graph, "A", 0, 0);
    B = new IntersectionVertex(graph, "B", 0.01, 0);

    VehicleParkingTestUtil.createStreet(A, B, StreetTraversalPermission.PEDESTRIAN);
  }

  public Graph getGraph() {
    return graph;
  }

  public TransitModel getTransitModel() {
    return transitModel;
  }

  public IntersectionVertex getAVertex() {
    return A;
  }

  public IntersectionVertex getBVertex() {
    return B;
  }
}
