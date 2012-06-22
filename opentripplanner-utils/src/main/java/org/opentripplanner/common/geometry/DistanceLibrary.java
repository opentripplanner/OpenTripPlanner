package org.opentripplanner.common.geometry;

import com.vividsolutions.jts.geom.Coordinate;

public interface DistanceLibrary {

    public abstract double distance(Coordinate from, Coordinate to);

    public abstract double fastDistance(Coordinate from, Coordinate to);

    public abstract double distance(double lat1, double lon1, double lat2, double lon2);

    public abstract double fastDistance(double lat1, double lon1, double lat2, double lon2);

    public abstract double distance(double lat1, double lon1, double lat2, double lon2,
            double radius);

    /**
     * Approximated, fast and under-estimated equirectangular distance between two points.
     * Works only for small delta lat/lon, fall-back on exact distance if not the case.
     * See: http://www.movable-type.co.uk/scripts/latlong.html
     */
    public abstract double fastDistance(double lat1, double lon1, double lat2, double lon2,
            double radius);

}