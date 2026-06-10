package org.opentripplanner.street.model.elevation;

import java.util.LinkedList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;

public final class ElevationProfileSlicer {

  private ElevationProfileSlicer() {}

  /**
   * Slice an elevation profile to {@code [start, end]} along the edge. Returns {@code null}
   * when the resulting sub-profile would have fewer than two coordinates (e.g. a zero-length
   * slice or a slice that falls entirely between two coordinate samples).
   */
  public static PackedCoordinateSequence slice(
    PackedCoordinateSequence elevationProfile,
    double start,
    double end
  ) {
    if (elevationProfile == null) {
      return null;
    }

    if (start < 0) {
      start = 0;
    }

    Coordinate[] coordinateArray = elevationProfile.toCoordinateArray();
    double length = coordinateArray[coordinateArray.length - 1].x;
    if (end > length) {
      end = length;
    }

    double newLength = end - start;

    boolean started = false;
    Coordinate lastCoord = null;
    List<Coordinate> coordList = new LinkedList<>();
    for (Coordinate coord : coordinateArray) {
      if (coord.x >= start && !started) {
        started = true;

        if (lastCoord != null) {
          double run = coord.x - lastCoord.x;
          double p = (start - lastCoord.x) / run;
          double rise = coord.y - lastCoord.y;
          double newX = lastCoord.x + p * run - start;
          double newY = lastCoord.y + p * rise;

          if (p > 0 && p < 1) {
            coordList.add(new Coordinate(newX, newY));
          }
        }
      }

      if (started && coord.x >= start && coord.x <= end) {
        coordList.add(new Coordinate(coord.x - start, coord.y));
      }

      if (started && coord.x >= end) {
        if (lastCoord != null && lastCoord.x < end && coord.x > end) {
          double run = coord.x - lastCoord.x;
          // interpolate end coordinate
          double p = (end - lastCoord.x) / run;
          double rise = coord.y - lastCoord.y;
          double newY = lastCoord.y + p * rise;
          coordList.add(new Coordinate(newLength, newY));
        }
        break;
      }

      lastCoord = coord;
    }

    if (coordList.size() < 2) {
      return null;
    }

    Coordinate[] coordArr = new Coordinate[coordList.size()];
    return new PackedCoordinateSequence.Float(coordList.toArray(coordArr), 2);
  }
}
