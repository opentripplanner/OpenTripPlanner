package org.opentripplanner.common.geometry;

import com.vividsolutions.jts.geom.Coordinate;

public interface Pointlike {

    public double getLon();
    public double getLat();

    /** Distance in meters to the coordinate */
    public abstract double distance(Coordinate c);

    /** Distance in meters to the vertex */
    public abstract double distance(Pointlike p);

    /** Fast, slightly approximated, under-estimated distance in meters to the vertex */
    public abstract double fastDistance(Pointlike p);
    
}
