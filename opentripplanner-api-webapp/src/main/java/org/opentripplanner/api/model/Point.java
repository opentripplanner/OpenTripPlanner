package org.opentripplanner.api.model;

import java.util.logging.Logger;

import org.opentripplanner.util.Constants;

/**
 *
 */
public class Point {

    protected static final Logger LOGGER = Logger.getLogger(Point.class.getCanonicalName());

    public String name = null;
    public String city = null;
    public String geometry = null;

    private Double lon = null;
    private Double lat = null;
    private Double x = null;
    private Double y = null;

    public Point() {
    }

    public Point(Double X, Double Y) {
        if (X == null)
            X = -0.0;
        if (Y == null)
            Y = -0.0;

        if (X <= 90.0 && Y <= 180.0) {
            setLon(X);
            setLat(Y);
            geometry = llGeoJSON();
        } else {
            setX(X);
            setY(Y);
            geometry = xyGeoJSON();
        }
    }

    public Point(Double X, Double Y, String n) {
        this(X, Y, n, null);
    }

    public Point(Double X, Double Y, String n, String c) {
        this(X, Y);
        name = n;
        city = c;
    }

    public void setName(String n) {
        name = n;
    }

    public void setCity(String c) {
        city = c;
    }

    public static boolean looksLikeCoordinate(String str) {
        if (str != null
                && (str.contains(Constants.POINT_PREFIX) || str
                        .matches("[\\s]*[0-9\\-.]+[,\\s]+[0-9\\-.]+[\\s]*")))
            return true;

        return false;
    }

    public boolean isSemiComplete() {
        return (((x != null && y != null) || (lat != null && lon != null)) && name != null);
    }

    public String xyGeoJSON() {
        return Constants.GEO_JSON + x + ", " + y + Constants.GEO_JSON_TAIL;
    }

    public String llGeoJSON() {

        return Constants.GEO_JSON + lat + ", " + lon + Constants.GEO_JSON_TAIL;
    }

    public void setLat(Double l) {
        lat = l;
        if (y == null)
            y = l;

        geometry = llGeoJSON();
    }

    public void setLon(Double l) {
        lon = l;
        if (x == null)
            x = l;

        geometry = llGeoJSON();
    }

    public void setY(Double Y) {
        y = Y;
        if (lat == null)
            lat = Y;

        geometry = xyGeoJSON();
    }

    public void setX(Double X) {
        x = X;
        if (lon == null)
            lon = X;

        geometry = xyGeoJSON();
    }
}
