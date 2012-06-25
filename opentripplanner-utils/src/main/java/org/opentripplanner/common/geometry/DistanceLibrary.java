package org.opentripplanner.common.geometry;

import com.vividsolutions.jts.geom.Coordinate;

public interface DistanceLibrary {

    public abstract double distance(Coordinate from, Coordinate to);

    public abstract double fastDistance(Coordinate from, Coordinate to);

    public abstract double distance(double lat1, double lon1, double lat2, double lon2);

    public abstract double fastDistance(double lat1, double lon1, double lat2, double lon2);

    public abstract double distance(double lat1, double lon1, double lat2, double lon2,
            double radius);


}