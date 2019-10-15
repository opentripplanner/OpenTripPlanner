package org.opentripplanner.graph_builder.module.geometry;

import org.apache.commons.math3.util.FastMath;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;

/** TODO Move this stuff into the geometry library */
class IndexedLineSegment {
    private static final double RADIUS = SphericalDistanceLibrary.RADIUS_OF_EARTH_IN_M;
    int index;
    Coordinate start;
    Coordinate end;
    private double lineLength;

    public IndexedLineSegment(int index, Coordinate start, Coordinate end) {
        this.index = index;
        this.start = start;
        this.end = end;
        this.lineLength = SphericalDistanceLibrary.fastDistance(start, end);
    }

    // in radians
    static double bearing(Coordinate c1, Coordinate c2) {
        double deltaLon = (c2.x - c1.x) * FastMath.PI / 180;
        double lat1Radians = c1.y * FastMath.PI / 180;
        double lat2Radians = c2.y * FastMath.PI / 180;
        double y = FastMath.sin(deltaLon) * FastMath.cos(lat2Radians);
        double x = FastMath.cos(lat1Radians)*FastMath.sin(lat2Radians) -
                FastMath.sin(lat1Radians)*FastMath.cos(lat2Radians)*FastMath.cos(deltaLon);
        return FastMath.atan2(y, x);
    }

    double crossTrackError(Coordinate coord) {
        double distanceFromStart = SphericalDistanceLibrary.fastDistance(start, coord);
        double bearingToCoord = bearing(start, coord);
        double bearingToEnd = bearing(start, end);
        return FastMath.asin(FastMath.sin(distanceFromStart / RADIUS)
            * FastMath.sin(bearingToCoord - bearingToEnd))
            * RADIUS;
    }

    double distance(Coordinate coord) {
        double cte = crossTrackError(coord);
        double atd = alongTrackDistance(coord, cte);
        double inverseAtd = inverseAlongTrackDistance(coord, -cte);
        double distanceToStart = SphericalDistanceLibrary.fastDistance(coord, start);
        double distanceToEnd = SphericalDistanceLibrary.fastDistance(coord, end);

        if (distanceToStart < distanceToEnd) {
            //we might be behind the line start
            if (inverseAtd > lineLength) {
                //we are behind line start
                return distanceToStart;
            } else {
                //we are within line
                return Math.abs(cte);
            }
        } else {
            //we might be after line end
            if (atd > lineLength) {
                //we are behind line end, so we that's the nearest point
                return distanceToEnd;
            } else {
                //we are within line
                return Math.abs(cte);
            }
        }
    }

    private double inverseAlongTrackDistance(Coordinate coord, double inverseCrossTrackError) {
        double distanceFromEnd = SphericalDistanceLibrary.fastDistance(end, coord);
        double alongTrackDistance = FastMath.acos(FastMath.cos(distanceFromEnd / RADIUS)
            / FastMath.cos(inverseCrossTrackError / RADIUS))
            * RADIUS;
        return alongTrackDistance;
    }

    public double fraction(Coordinate coord) {
        double cte = crossTrackError(coord);
        double distanceToStart = SphericalDistanceLibrary.fastDistance(coord, start);
        double distanceToEnd = SphericalDistanceLibrary.fastDistance(coord, end);

        if (cte < distanceToStart && cte < distanceToEnd) {
            double atd = alongTrackDistance(coord, cte);
            return atd / lineLength;
        } else {
            if (distanceToStart < distanceToEnd) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    private double alongTrackDistance(Coordinate coord, double crossTrackError) {
        double distanceFromStart = SphericalDistanceLibrary.fastDistance(start, coord);
        double alongTrackDistance = FastMath.acos(FastMath.cos(distanceFromStart / RADIUS)
            / FastMath.cos(crossTrackError / RADIUS))
            * RADIUS;
        return alongTrackDistance;
    }
}
