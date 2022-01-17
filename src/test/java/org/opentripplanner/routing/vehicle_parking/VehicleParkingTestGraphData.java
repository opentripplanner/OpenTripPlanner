package org.opentripplanner.routing.vehicle_parking;

import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

public class VehicleParkingTestGraphData {

    protected IntersectionVertex A, B;

    protected Graph graph;

    public void initGraph() {
        graph = new Graph();
        graph.hasStreets = true;

        A = new IntersectionVertex(graph, "A", 0, 0);
        B = new IntersectionVertex(graph, "B", 0.01, 0);

        VehicleParkingTestUtil.createStreet(A, B, StreetTraversalPermission.PEDESTRIAN);
    }

    public Graph getGraph() {
        return graph;
    }

    public IntersectionVertex getAVertex() {
        return A;
    }

    public IntersectionVertex getBVertex() {
        return B;
    }
}
