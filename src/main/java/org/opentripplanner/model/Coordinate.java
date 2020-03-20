package org.opentripplanner.model;

import org.opentripplanner.model.base.ValueObjectToStringBuilder;
import org.opentripplanner.util.CoordinateUtils;

import java.io.Serializable;

/**
 * This class represent a OTP coordinate.
 * <p>
 * This is a ValueObject (design pattern).
 */
public final class Coordinate implements Serializable {
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

    /**
     * Compare to coordinates, normalize both using an EPSILON of 1E-7.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        Coordinate that = (Coordinate) o;
        return CoordinateUtils.coordinateEquals(lat, lon, that.lat, that.lon);
    }

    /**
     * Compute the normalized hash, using the same EPSILON of 1E-7 as in the compare function.
     */
    @Override
    public int hashCode() {
        return CoordinateUtils.hash(lat, lon);
    }

    /**
     * Return a string on the form: {@code "(60.12345, 11.12345)"}. Up to 5 digits are used
     * after the period(.), even if the coordinate is specified with a higher precision.
     */
    @Override
    public String toString() {
        return ValueObjectToStringBuilder.of().addCoordinate(latitude(), longitude()).toString();
    }
}
