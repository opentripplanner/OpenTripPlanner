package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import org.locationtech.jts.geom.Coordinate;

import java.util.List;

public class Transfer {
    private int stop;

    private final int distance;

    private final List<Coordinate> coordinates;

    public Transfer(int stop, int distance, List<Coordinate> coordinates) {
        this.stop = stop;
        this.distance = distance;
        this.coordinates = coordinates;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    public int stop() { return stop; }

    public int getDistance() {
        return distance;
    }
}
