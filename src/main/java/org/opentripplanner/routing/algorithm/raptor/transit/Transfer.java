package org.opentripplanner.routing.algorithm.raptor.transit;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.graph.Edge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Transfer {
    private int toStop;

    // TODO OTP2: we only have a distance field here but in other cases have more detail.
    // Inside Raptor we need distance to apply walking speed on the fly to precomputed transfers;
    // Outside raptor (in access and egress searches) we can actually have true travel times, because we compute
    // throw-away request-scoped transfers that are specific to a single set of routing parameters.
    // Keeping the transfers simplified to only distances which are scaled at search time makes it impossible to
    // include slowdown due to slope, for example. We could store an "effective distance" which bakes the non-linear
    // slope (and other) components into the distance, and then the linear speed scaling can be applied fast in routing.
    private final int distanceMeters;

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
