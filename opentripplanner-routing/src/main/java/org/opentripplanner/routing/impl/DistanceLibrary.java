package org.opentripplanner.routing.impl;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

public class DistanceLibrary {

    public static final double RADIUS_OF_EARTH_IN_KM = 6371.01;

    public static final double distance(double lat1, double lon1, double lat2, double lon2) {
        return distance(lat1, lon1, lat2, lon2, RADIUS_OF_EARTH_IN_KM * 1000);
    }

    public static final double distance(double lat1, double lon1, double lat2, double lon2,
            double radius) {

        // http://en.wikipedia.org/wiki/Great-circle_distance
        lat1 = toRadians(lat1); // Theta-s
        lon1 = toRadians(lon1); // Lambda-s
        lat2 = toRadians(lat2); // Theta-f
        lon2 = toRadians(lon2); // Lambda-f

        double deltaLon = lon2 - lon1;

        double y = sqrt(DistanceLibrary.p2(cos(lat2) * sin(deltaLon))
                + DistanceLibrary.p2(cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)));
        double x = sin(lat1) * sin(lat2) + cos(lat1) * cos(lat2) * cos(deltaLon);

        return radius * atan2(y, x);
    }

    private static final double p2(double a) {
        return a * a;
    }
}
