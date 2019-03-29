package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import org.locationtech.jts.geom.Coordinate;

import java.util.List;

public class Transfer {
    private int toStop;

    private final int distance; // TODO Add units in the name of the field

    private final List<Coordinate> coordinates;

    public Transfer(int toStop, int distance, List<Coordinate> coordinates) {
        this.toStop = toStop;
        this.distance = distance;
        this.coordinates = coordinates;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    //TODO getToStop
    public int stop() { return toStop; }

    public int getDistance() {
        return distance;
    }
}
