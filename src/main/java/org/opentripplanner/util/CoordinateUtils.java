package org.opentripplanner.util;

public class CoordinateUtils {

    /**
     * A epsilon of 1E-7 gives a precision for coordinates at equator at 1.1 cm,
     * witch is good enough for compering most coordinates in OTP.
     */
    private static final double EPSILON = 1E-7;

    /**
     * Coordinates are considered equals if they are within a box 0.11 x 0.11 meter at equator.
     * The precision will increase for the longitude when moving north or south.
     */
    public static boolean compare(double longitudeA, double latitudeA, double longitudeB, double latitudeB) {
        return isEquals(longitudeA, longitudeB) && isEquals(latitudeA, latitudeB);
    }

    private static boolean isEquals(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }
}
