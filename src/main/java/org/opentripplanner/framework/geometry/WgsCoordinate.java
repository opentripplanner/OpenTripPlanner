package org.opentripplanner.framework.geometry;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.framework.lang.DoubleUtils;
import org.opentripplanner.framework.tostring.ValueObjectToStringBuilder;

/**
 * This class represent a OTP coordinate.
 * <p>
 * This is a ValueObject (design pattern).
 */
public final class WgsCoordinate implements Serializable {

  public static final WgsCoordinate GREENWICH = new WgsCoordinate(51.48, 0d);
  private static final double LAT_MIN = -90;
  private static final double LAT_MAX = 90;
  private static final double LON_MIN = -180;
  private static final double LON_MAX = 180;

  private final double latitude;
  private final double longitude;

  public WgsCoordinate(double latitude, double longitude) {
    // Verify coordinates are in range
    DoubleUtils.requireInRange(latitude, LAT_MIN, LAT_MAX, "latitude");
    DoubleUtils.requireInRange(longitude, LON_MIN, LON_MAX, "longitude");

    // Normalize coordinates to precision around ~ 1 centimeters (7 decimals)
    this.latitude = DoubleUtils.roundTo7Decimals(latitude);
    this.longitude = DoubleUtils.roundTo7Decimals(longitude);
  }

  public WgsCoordinate(Point point) {
    Objects.requireNonNull(point);
    this.latitude = DoubleUtils.roundTo7Decimals(point.getY());
    this.longitude = DoubleUtils.roundTo7Decimals(point.getX());
  }

  public WgsCoordinate(Coordinate coordinate) {
    Objects.requireNonNull(coordinate);
    this.latitude = DoubleUtils.roundTo7Decimals(coordinate.getY());
    this.longitude = DoubleUtils.roundTo7Decimals(coordinate.getX());
  }

  /**
   * Unlike the constructor this factory method retuns {@code null} if both {@code lat} and {@code
   * lon} is {@code null}.
   */
  public static WgsCoordinate creatOptionalCoordinate(Double latitude, Double longitude) {
    if (latitude == null && longitude == null) {
      return null;
    }

    // Set coordinate is both lat and lon exist
    if (latitude != null && longitude != null) {
      return new WgsCoordinate(latitude, longitude);
    }
    throw new IllegalArgumentException(
      "Both 'latitude' and 'longitude' must have a value or both must be 'null'."
    );
  }

  /**
   * Find the mean coordinate between the given set of {@code coordinates}.
   */
  public static WgsCoordinate mean(Collection<WgsCoordinate> coordinates) {
    if (coordinates.isEmpty()) {
      throw new IllegalArgumentException(
        "Unable to calculate mean for an empty set of coordinates"
      );
    }
    if (coordinates.size() == 1) {
      return coordinates.iterator().next();
    }

    double n = coordinates.size();
    double latitude = 0.0;
    double longitude = 0.0;

    for (WgsCoordinate c : coordinates) {
      latitude += c.latitude();
      longitude += c.longitude();
    }

    return new WgsCoordinate(latitude / n, longitude / n);
  }

  public double latitude() {
    return latitude;
  }

  public double longitude() {
    return longitude;
  }

  /** Return OTP domain coordinate as JTS GeoTools Library coordinate. */
  public Coordinate asJtsCoordinate() {
    return new Coordinate(longitude, latitude);
  }

  /**
   * Compare to coordinates and return {@code true} if they are close together - have the same
   * location. The comparison uses an EPSILON of 1E-7 for each axis, for both latitude and
   * longitude.
   *
   * When we compare two coordinates we want to see if they are within a given distance,
   * roughly within a square centimeter. This is not
   * <em>transitive</em>, hence violating the equals/hasCode guideline. Consider 3 point along
   * one of the axis:
   * <pre>
   *      | 8mm | 8mm |
   *      x --- y --- z
   *     </pre>
   * Then {@code x.sameLocation(y)} is {@code true} and {@code y.sameLocation(z)} is {@code true},
   * but {@code x.sameLocation(z)} is {@code false}.
   */
  public boolean sameLocation(WgsCoordinate other) {
    return equals(other);
  }

  /**
   * Add (deltaLat, deltaLon) to the current coordinates and return the new position.
   */
  public WgsCoordinate add(double deltaLat, double deltaLon) {
    return new WgsCoordinate(latitude + deltaLat, longitude + deltaLon);
  }

  /**
   * Return a new version of this coordinate where latitude/longitude are rounded to 4 decimal
   * places which at the equator has ~11 meter precision.
   * <p>
   * See https://wiki.openstreetmap.org/wiki/Precision_of_coordinates
   * <p>
   * This is useful when you want to cache coordinate-based computations but don't need absolute
   * precision.
   * <p>
   * DO NOT USE THIS IN ROUTING (USE AT LEAST 7 DECIMALS)!
   */
  public WgsCoordinate roundToApproximate10m() {
    var lat = DoubleUtils.roundTo4Decimals(latitude);
    var lng = DoubleUtils.roundTo4Decimals(longitude);
    return new WgsCoordinate(lat, lng);
  }

  /**
   * Return a new version of this coordinate where latitude/longitude are rounded to 3 decimal
   * places which at the equator has ~100 meter precision.
   * <p>
   * See https://wiki.openstreetmap.org/wiki/Precision_of_coordinates
   * <p>
   * This is useful when you want to cache coordinate-based computations but don't need absolute
   * precision.
   * <p>
   * DO NOT USE THIS IN ROUTING (USE AT LEAST 7 DECIMALS)!
   */
  public WgsCoordinate roundToApproximate100m() {
    var lat = DoubleUtils.roundTo3Decimals(latitude);
    var lng = DoubleUtils.roundTo3Decimals(longitude);
    return new WgsCoordinate(lat, lng);
  }

  /**
   * Return a string on the form: {@code "(60.12345, 11.12345)"}. Up to 5 digits are used after the
   * period(.), even if the coordinate is specified with a higher precision.
   */
  @Override
  public String toString() {
    return ValueObjectToStringBuilder.of().addCoordinate(latitude(), longitude()).toString();
  }

  /**
   * Return true if the coordinates are numerically equal. The coordinate latitude and longitude
   * are rounded to the closest number of 1E-7 when constructed. This enforces two coordinates
   * that is close together to be equals.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WgsCoordinate other = (WgsCoordinate) o;
    return latitude == other.latitude && longitude == other.longitude;
  }

  @Override
  public int hashCode() {
    return Objects.hash(latitude, longitude);
  }
}
