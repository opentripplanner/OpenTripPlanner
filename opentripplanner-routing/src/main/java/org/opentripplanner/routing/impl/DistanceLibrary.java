package org.opentripplanner.routing.impl;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

import org.geotools.factory.Hints;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;

public class DistanceLibrary {

    public static final CoordinateReferenceSystem WORLD_CRS;

    static {
        try {

            Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
            CRSAuthorityFactory factory = ReferencingFactoryFinder.getCRSAuthorityFactory("EPSG",
                    hints);
            WORLD_CRS = factory
                    .createCoordinateReferenceSystem("EPSG:4326");
        } catch (Exception ex) {
            throw new IllegalStateException("error creating EPSG:4326 coordinate ref system", ex);
        }
    }

    public static double orthodromicDistance(Coordinate a, Coordinate b) {
        try {
            return JTS.orthodromicDistance(a,b,WORLD_CRS);
        } catch (TransformException ex) {
            throw new IllegalStateException("error computing distance: a=" + a + " b=" + b,ex);
        }
    }
    
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
