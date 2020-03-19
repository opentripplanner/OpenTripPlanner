package org.opentripplanner.model;

import org.opentripplanner.model.base.ValueObjectToStringBuilder;
import org.opentripplanner.util.CoordinateUtils;

import java.io.Serializable;
import java.util.Objects;

public class Coordinate implements Serializable {
    private final double lat;
    private final double lon;

    public Coordinate(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double latitude() {
        return lat;
    }

    public double longitude() {
        return lon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        Coordinate that = (Coordinate) o;
        return CoordinateUtils.compare(lat, lon, that.lat, that.lon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lon);
    }

    @Override
    public String toString() {
        return ValueObjectToStringBuilder.of().addCoordinate(lat, lon).toString();
    }
}
