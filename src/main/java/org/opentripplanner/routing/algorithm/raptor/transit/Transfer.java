package org.opentripplanner.routing.algorithm.raptor.transit;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.graph.Edge;

import java.util.Collections;
import java.util.List;

public class Transfer {
    private int toStop;

    private final int distanceMeters; // TODO Add units in the name of the field

    private final List<Coordinate> coordinates;

    private final List<Edge> edges;

    public Transfer(int toStop, int distanceMeters, List<Coordinate> coordinates) {
        this.toStop = toStop;
        this.distanceMeters = distanceMeters;
        this.coordinates = coordinates;
        this.edges = Collections.emptyList();
    }

    public Transfer(int toStop, int distanceMeters, List<Coordinate> coordinates, List<Edge> edges) {
        this.toStop = toStop;
        this.distanceMeters = distanceMeters;
        this.coordinates = coordinates;
        this.edges = edges;
    }

    public List<Coordinate> getCoordinates() {
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
