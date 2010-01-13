package org.opentripplanner.common.geometry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class DirectionUtils {


    /**
     * Computes the angle of the last segment of a LineString or MultiLineString
     *
     * @param geometry
     *            a LineString or a MultiLineString
     * @return
     */
    public static double getLastAngle(Geometry geometry) {
        LineString line;
        if (geometry instanceof MultiLineString) {
            line = (LineString) geometry.getGeometryN(geometry.getNumGeometries() - 1);
        } else {
            assert geometry instanceof LineString;
            line = (LineString) geometry;
        }
        int numPoints = line.getNumPoints();
        Coordinate coord0 = line.getCoordinateN(numPoints - 2);
        Coordinate coord1 = line.getCoordinateN(numPoints - 1);
        return Math.atan2(coord1.y - coord0.y, coord1.x - coord0.x);
    }

    /**
     * Computes the angle of the first segment of a LineString or MultiLineString
     *
     * @param geometry
     *            a LineString or a MultiLineString
     * @return
     */
    public static double getFirstAngle(Geometry geometry) {
        LineString line;
        if (geometry instanceof MultiLineString) {
            line = (LineString) geometry.getGeometryN(0);
        } else {
            assert geometry instanceof LineString;
            line = (LineString) geometry;
        }

        Coordinate coord0 = line.getCoordinateN(0);
        Coordinate coord1 = line.getCoordinateN(1);
        return Math.atan2(coord1.y - coord0.y, coord1.x - coord0.x);
    }
}
