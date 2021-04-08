package org.opentripplanner.routing.algorithm.raptor.transit;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.graph.Edge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Transfer {
    private int toStop;

    private final int effectiveWalkDistanceMeters;
    private final int distanceIndependentTime;

    private final List<Edge> edges;

    public Transfer(
        int toStop, int effectiveWalkDistanceMeters, int distanceIndependentTime, List<Edge> edges
    ) {
        this.toStop = toStop;
        this.effectiveWalkDistanceMeters = effectiveWalkDistanceMeters;
        this.distanceIndependentTime = distanceIndependentTime;
        this.edges = edges;
    }

    public List<Coordinate> getCoordinates() {
        List<Coordinate> coordinates = new ArrayList<>();
        if (edges == null) { return coordinates; }
        for (Edge edge : edges) {
            if (edge.getGeometry() != null) {
                coordinates.addAll((Arrays.asList(edge.getGeometry().getCoordinates())));
            }
        }
        return coordinates;
    }

    public int getToStop() { return toStop; }

    /**
     * The effective distance is defined as the value that divided by the speed will give the
     * correct duration of the transfer. This takes into account slowdowns/speedups related to
     * slopes. It can also account for other factors that affect the transfer duration, but all
     * factors are required to be proportional to the speed. The reason we are doing this is so
     * that we can calculate all transfer durations in a reasonable amount of time for each
     * incoming request.
     */
    public int getEffectiveWalkDistanceMeters() {
        return effectiveWalkDistanceMeters;
    }

    public int getDistanceIndependentTime() {
        return distanceIndependentTime;
    }

    public int getDistanceMeters() {
        return edges != null
            ? (int) edges.stream().mapToDouble(Edge::getDistanceMeters).sum()
            : effectiveWalkDistanceMeters;
    }

    public List<Edge> getEdges() {
        return edges;
    }
}
