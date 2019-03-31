package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import org.locationtech.jts.geom.Coordinate;

import java.util.List;

public class Transfer {
    private int toStop;

    private final int distanceMeters; // TODO Add units in the name of the field

    private final List<Coordinate> coordinates;

    public Transfer(int toStop, int distanceMeters, List<Coordinate> coordinates) {
        this.toStop = toStop;
        this.distanceMeters = distanceMeters;
        this.coordinates = coordinates;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    //TODO getToStop
    public int stop() { return toStop; }

    public int getDistanceMeters() {
        return distanceMeters;
    }
}
