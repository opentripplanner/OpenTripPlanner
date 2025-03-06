package org.opentripplanner.graph_builder.module.geometry;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;

/** TODO Move this stuff into the geometry library */
class IndexedLineSegment {

  private static final double RADIUS = SphericalDistanceLibrary.RADIUS_OF_EARTH_IN_M;
  private final double lineLength;
  int index;
  Coordinate start;
  Coordinate end;

  public IndexedLineSegment(int index, Coordinate start, Coordinate end) {
    this.index = index;
    this.start = start;
    this.end = end;
    this.lineLength = SphericalDistanceLibrary.fastDistance(start, end);
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

  // in radians
  static double bearing(Coordinate c1, Coordinate c2) {
    double deltaLon = ((c2.x - c1.x) * Math.PI) / 180;
    double lat1Radians = (c1.y * Math.PI) / 180;
    double lat2Radians = (c2.y * Math.PI) / 180;
    double y = Math.sin(deltaLon) * Math.cos(lat2Radians);
    double x =
      Math.cos(lat1Radians) * Math.sin(lat2Radians) -
      Math.sin(lat1Radians) * Math.cos(lat2Radians) * Math.cos(deltaLon);
    return Math.atan2(y, x);
  }

  double crossTrackError(Coordinate coord) {
    double distanceFromStart = SphericalDistanceLibrary.fastDistance(start, coord);
    double bearingToCoord = bearing(start, coord);
    double bearingToEnd = bearing(start, end);
    return (
      Math.asin(Math.sin(distanceFromStart / RADIUS) * Math.sin(bearingToCoord - bearingToEnd)) *
      RADIUS
    );
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
    return (
      Math.acos(Math.cos(distanceFromEnd / RADIUS) / Math.cos(inverseCrossTrackError / RADIUS)) *
      RADIUS
    );
  }

  private double alongTrackDistance(Coordinate coord, double crossTrackError) {
    double distanceFromStart = SphericalDistanceLibrary.fastDistance(start, coord);
    return (
      Math.acos(Math.cos(distanceFromStart / RADIUS) / Math.cos(crossTrackError / RADIUS)) * RADIUS
    );
  }
}
