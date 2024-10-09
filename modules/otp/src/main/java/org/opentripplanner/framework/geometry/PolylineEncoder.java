package org.opentripplanner.framework.geometry;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * This encodes geometries to the Google Polyline encoding
 *
 * {@see EncodedPolyline}
 */
class PolylineEncoder {

  static EncodedPolyline encodeGeometry(Geometry geometry) {
    if (geometry instanceof LineString string) {
      return encodeCoordinates(string.getCoordinates());
    } else if (geometry instanceof MultiLineString mls) {
      return encodeCoordinates(mls.getCoordinates());
    } else if (geometry instanceof Polygon polygon) {
      return encodeCoordinates(polygon.getCoordinates());
    } else if (geometry instanceof Point point) {
      return encodeCoordinates(point.getCoordinates());
    } else {
      throw new IllegalArgumentException(geometry.toString());
    }
  }

  static EncodedPolyline encodeCoordinates(Coordinate[] points) {
    StringBuilder encodedPoints = new StringBuilder();

    int plat = 0;
    int plng = 0;
    int count = 0;

    for (Coordinate point : points) {
      int late5 = floor1e5(point.y);
      int lnge5 = floor1e5(point.x);

      int dlat = late5 - plat;
      int dlng = lnge5 - plng;

      plat = late5;
      plng = lnge5;

      encodedPoints.append(encodeSignedNumber(dlat)).append(encodeSignedNumber(dlng));
      count++;
    }

    return new EncodedPolyline(encodedPoints.toString(), count);
  }

  private static String encodeSignedNumber(int num) {
    int sgn_num = num << 1;
    if (num < 0) {
      sgn_num = ~(sgn_num);
    }
    return (encodeNumber(sgn_num));
  }

  private static String encodeNumber(int num) {
    StringBuilder encodeString = new StringBuilder();

    while (num >= 0x20) {
      int nextValue = (0x20 | (num & 0x1f)) + 63;
      encodeString.append((char) (nextValue));
      num >>= 5;
    }

    num += 63;
    encodeString.append((char) (num));

    return encodeString.toString();
  }

  private static int floor1e5(double coordinate) {
    return (int) Math.floor(coordinate * 1e5);
  }
}
