package org.opentripplanner.updater.transportation_network_company;

import java.util.Objects;

/**
 * This class is used for approximating a position.
 * It is used so that numerous TNC requests with very similar coordinates can be assumed to be the same.
 */
public class Position {
    public double latitude;
    public double longitude;

    private int intLat;
    private int intLon;

    public Position(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.intLat = intVal(latitude);
        this.intLon = intVal(longitude);
    }

    public int getIntLat() {
        return intLat;
    }

    public int getIntLon() {
        return intLon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return intLat == position.getIntLat() &&
            intLon == position.getIntLon();
    }

    @Override
    public int hashCode() {
        return Objects.hash(intLat, intLon);
    }

    public int intVal (double d) {
        return (int) (d * 10000);
    }
}
