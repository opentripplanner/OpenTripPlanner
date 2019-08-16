package org.opentripplanner.routing.algorithm.raptor.transit;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.graph.Edge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Transfer {
    private int toStop;

    private final int distanceMeters; // TODO Add units in the name of the field

    private final List<Edge> edges;

    public Transfer(int toStop, int distanceMeters) {
        this.toStop = toStop;
        this.distanceMeters = distanceMeters;
        this.edges = Collections.emptyList();
    }

    public Transfer(int toStop, int distanceMeters, List<Edge> edges) {
        this.toStop = toStop;
        this.distanceMeters = distanceMeters;
        this.edges = edges;
    }

    public List<Coordinate> getCoordinates() {
        List<Coordinate> coordinates = new ArrayList<>();
        for (Edge edge : edges) {
            if (edge.getGeometry() != null) {
                coordinates.addAll((Arrays.asList(edge.getGeometry().getCoordinates())));
            }
        }
        return coordinates;
    }

    public int getToStop() { return toStop; }

    public int getDistanceMeters() {
        return distanceMeters;
    }

    public List<Edge> getEdges() {
        return edges;
    }
}
