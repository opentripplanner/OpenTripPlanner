package org.opentripplanner.service.vehicleparking;

import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.streetadapter.VertexFactory;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;

public class VehicleParkingTestGraphData {

  protected IntersectionVertex A;
  protected IntersectionVertex B;

  protected Graph graph;

  protected TimetableRepository timetableRepository;

  public void initGraph() {
    var siteRepository = new SiteRepository();
    graph = new Graph();
    timetableRepository = new TimetableRepository(siteRepository);
    graph.hasStreets = true;

    var factory = new VertexFactory(graph);

    A = factory.intersection("A", 0, 0);
    B = factory.intersection("B", 0.01, 0);

    StreetModelForTest.streetEdge(A, B, StreetTraversalPermission.PEDESTRIAN);
  }

  public Graph getGraph() {
    return graph;
  }

  public TimetableRepository getTimetableRepository() {
    return timetableRepository;
  }

  public IntersectionVertex getAVertex() {
    return A;
  }

  public IntersectionVertex getBVertex() {
    return B;
  }
}
