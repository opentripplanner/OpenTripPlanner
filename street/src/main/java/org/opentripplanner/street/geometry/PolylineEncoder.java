package org.opentripplanner.street.geometry;

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
public class PolylineEncoder {

  /**
   * Encodes a JTS Geometry into Google Polyline format.
   * <p>
   * Supports LineString, MultiLineString, Polygon, and Point geometries by extracting their
   * coordinates and encoding them using the Google Polyline encoding algorithm.
   *
   * @param geometry the JTS Geometry to encode (LineString, MultiLineString, Polygon, or Point)
   * @return a PolylineEncoderResult containing the encoded polyline string and the number of
   *     coordinates
   * @throws IllegalArgumentException if the geometry type is not supported
   */
  public static PolylineEncoderResult encodeGeometry(Geometry geometry) {
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

  /**
   * Encodes an array of coordinates using the Google Polyline encoding algorithm.
   * <p>
   * The algorithm works by:
   * <ol>
   *   <li>Converting each coordinate (latitude/longitude) to a fixed-precision integer by
   *       multiplying by 10^5</li>
   *   <li>Computing deltas (differences) from the previous point to reduce the magnitude of
   *       numbers (the first point uses absolute coordinates)</li>
   *   <li>Encoding each delta as a signed integer using bit manipulation</li>
   *   <li>Converting the result to ASCII characters for compact string representation</li>
   * </ol>
   * This approach provides lossy compression that significantly reduces the space needed to store
   * coordinate sequences while maintaining sufficient precision (approximately 1 meter) for most
   * mapping applications.
   *
   * @param points array of JTS Coordinates to encode
   * @return a PolylineEncoderResult containing the encoded string and the number of points encoded
   */
  public static PolylineEncoderResult encodeCoordinates(Coordinate[] points) {
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

    return new PolylineEncoderResult(encodedPoints.toString(), count);
  }

  /**
   * Encodes a signed integer using the Google Polyline encoding scheme.
   * <p>
   * The encoding process:
   * <ol>
   *   <li>Left-shift the number by 1 bit (equivalent to multiplying by 2)</li>
   *   <li>If the number is negative, invert all bits using the bitwise NOT operator (~).
   *       This ensures negative values are distinguishable from positive values.</li>
   *   <li>Pass the result to encodeNumber() for conversion to 5-bit chunks and ASCII</li>
   * </ol>
   * <p>
   * This approach encodes the sign information in the least significant bit after the left shift:
   * positive numbers have their bits shifted left (LSB = 0), while negative numbers have their
   * bits shifted left and then inverted (LSB = 1 after inversion).
   *
   * @param num the signed integer to encode (typically a coordinate delta)
   * @return the encoded string representation
   */
  private static String encodeSignedNumber(int num) {
    int sgn_num = num << 1;
    if (num < 0) {
      sgn_num = ~(sgn_num);
    }
    return (encodeNumber(sgn_num));
  }

  /**
   * Encodes an unsigned integer into Google Polyline format by breaking it into 5-bit chunks.
   * <p>
   * The encoding algorithm:
   * <ol>
   *   <li>Extract the least significant 5 bits of the number using bitwise AND with 0x1f (31)</li>
   *   <li>If more bits remain (num >= 0x20), set the 6th bit to 1 by OR-ing with 0x20.
   *       This continuation bit signals that more chunks follow.</li>
   *   <li>Add 63 to the result to shift into the printable ASCII range (63-126)</li>
   *   <li>Convert to a character and append to the result string</li>
   *   <li>Right-shift the number by 5 bits and repeat until num < 0x20 (32)</li>
   *   <li>For the final chunk (no continuation), just add 63 and convert to character</li>
   * </ol>
   * <p>
   * This produces a variable-length ASCII string where each character represents 5 bits of data,
   * with the 6th bit indicating whether more characters follow. The result is a compact
   * representation suitable for URLs and JSON.
   *
   * @param num the unsigned integer to encode (must be non-negative)
   * @return the encoded string of ASCII characters
   */
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

  /**
   * Converts a decimal coordinate to a fixed-precision integer representation.
   * <p>
   * Multiplies the coordinate by 10^5 (100,000) and takes the floor to achieve a precision of
   * 5 decimal places. This provides approximately 1.1 meter precision at the equator, which is
   * sufficient for most mapping applications.
   */
  private static int floor1e5(double coordinate) {
    return (int) Math.floor(coordinate * 1e5);
  }

  /**
   * Result of polyline encoding containing the encoded string and metadata.
   *
   * @param points the encoded polyline string in Google Polyline format
   * @param length the number of coordinates that were encoded
   */
  public record PolylineEncoderResult(String points, int length) {}
}
