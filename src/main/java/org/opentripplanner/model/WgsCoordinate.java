package org.opentripplanner.model;

import org.opentripplanner.model.base.ValueObjectToStringBuilder;

import java.io.Serializable;

/**
 * This class represent a OTP coordinate.
 * <p>
 * This is a ValueObject (design pattern).
 */
public final class WgsCoordinate implements Serializable {
    private static final String WHY_COORDINATE_DO_NOT_HAVE_HASH_EQUALS =
            "Use the 'sameLocation(..)' method to compare coordinates. See JavaDoc on 'equals(..)'";

    /**
     * A epsilon of 1E-7 gives a precision for coordinates at equator at 1.1 cm,
     * witch is good enough for compering most coordinates in OTP.
     */
    private static final double EPSILON = 1E-7;



    private final double latitude;
    private final double longitude;

    public WgsCoordinate(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Unlike the constructor this factory method retuns {@code null} if both {@code lat} and
     * {@code lon} is {@code null}.
     */
    public static WgsCoordinate creatOptionalCoordinate(Double latitude, Double longitude) {
        if(latitude == null && longitude == null) { return null; }

        // Set coordinate is both lat and lon exist
        if(latitude != null && longitude != null) {
            return new WgsCoordinate(latitude, longitude);
        }
        throw new IllegalArgumentException(
                "Both 'latitude' and 'longitude' must have a value or both must be 'null'."
        );
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    /**
     * Compare to coordinates and return {@code true} if they are close together - have the
     * same location. The comparison uses an EPSILON of 1E-7 for each axis, for both latitude and
     * longitude.
     */
    public boolean sameLocation(WgsCoordinate other) {
        if (this == other) { return true; }
        return isCloseTo(latitude, other.latitude) &&
               isCloseTo(longitude, other.longitude);
    }

    /**
     * Not supported, throws UnsupportedOperationException. When we compare to coordinates we want
     * see if they are within a given distance, roughly within a square centimeter. This is not
     * <em>transitive</em>, hence violating the equals/hasCode guideline. Consider 3 point along
     * one of the axis:
     * <pre>
     *  | 8mm | 8mm |
     *  x --- y --- z
     * </pre>
     * Then {@code x.sameLocation(y)} is {@code true} and {@code y.sameLocation(z)} is
     * {@code true}, but {@code x.sameLocation(z)} is {@code false}.
     * <p>
     * Use the {@link #sameLocation(WgsCoordinate)} method instead of equals, and never put this
     * class in a Set or use it as a key in a Map.
     * @throws UnsupportedOperationException if called.
     */
    @Override
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException(WHY_COORDINATE_DO_NOT_HAVE_HASH_EQUALS);
    }

    /**
     * @throws UnsupportedOperationException if called. See {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        throw new UnsupportedOperationException(WHY_COORDINATE_DO_NOT_HAVE_HASH_EQUALS);
    }

    /**
     * Return a string on the form: {@code "(60.12345, 11.12345)"}. Up to 5 digits are used
     * after the period(.), even if the coordinate is specified with a higher precision.
     */
    @Override
    public String toString() {
        return ValueObjectToStringBuilder.of().addCoordinate(latitude(), longitude()).toString();
    }

    private static boolean isCloseTo(double a, double b) {
        double delta = Math.abs(a - b);
        return delta < EPSILON;
    }
}
