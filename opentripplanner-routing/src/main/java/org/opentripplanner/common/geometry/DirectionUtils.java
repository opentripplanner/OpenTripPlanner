package org.opentripplanner.common.geometry;

import org.geotools.referencing.GeodeticCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class DirectionUtils {

    public static DirectionUtils instance;
    /* this is used to calculate angles on a sphere */
    private GeodeticCalculator geodeticCalculator;

    private DirectionUtils() {
        geodeticCalculator = new GeodeticCalculator();
    }

    public static DirectionUtils getInstance() {
        if (instance == null) {
            instance = new DirectionUtils();
        }
        return instance;
    }

    /**
     * Computes the angle of the last segment of a LineString or MultiLineString
     *
     * @param geometry
     *            a LineString or a MultiLineString
     * @return
     */
    public double getLastAngle(Geometry geometry) {
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

        geodeticCalculator.setStartingGeographicPoint(coord0.x, coord0.y);
        geodeticCalculator.setDestinationGeographicPoint(coord1.x, coord1.y);
        return geodeticCalculator.getAzimuth() * Math.PI / 180;
    }

    /**
     * Computes the angle of the first segment of a LineString or MultiLineString
     *
     * @param geometry
     *            a LineString or a MultiLineString
     * @return
     */
    public double getFirstAngle(Geometry geometry) {
        LineString line;
        if (geometry instanceof MultiLineString) {
            line = (LineString) geometry.getGeometryN(0);
        } else {
            assert geometry instanceof LineString;
            line = (LineString) geometry;
        }

        Coordinate coord0 = line.getCoordinateN(0);
        Coordinate coord1 = line.getCoordinateN(1);

        geodeticCalculator.setStartingGeographicPoint(coord0.x, coord0.y);
        geodeticCalculator.setDestinationGeographicPoint(coord1.x, coord1.y);
        return geodeticCalculator.getAzimuth() * Math.PI / 180;
    }
}
