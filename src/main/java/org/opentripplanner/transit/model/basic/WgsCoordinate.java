package org.opentripplanner.transit.model.basic;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.util.lang.ValueObjectToStringBuilder;

/**
 * This class represent a OTP coordinate.
 * <p>
 * This is a ValueObject (design pattern).
 */
public final class WgsCoordinate implements Serializable {

  /**
   * A epsilon of 1E-7 gives a precision for coordinates at equator at 1.1 cm, which is good enough
   * for compering most coordinates in OTP.
   */
  private static final double EPSILON = 1E-7;

  private final double latitude;
  private final double longitude;

  public WgsCoordinate(double latitude, double longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
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
    if (this == other) {
      return true;
    }
    return isCloseTo(latitude, other.latitude) && isCloseTo(longitude, other.longitude);
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
   * Return true if the coordinates are numerically equal.
   * Use {@link #sameLocation(WgsCoordinate)} instead for checking if coordinates are so close
   * to each other that they are geographically identical for all practical purposes.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WgsCoordinate that = (WgsCoordinate) o;
    return (
      Double.compare(that.latitude, latitude) == 0 && Double.compare(that.longitude, longitude) == 0
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(latitude, longitude);
  }

  private static boolean isCloseTo(double a, double b) {
    double delta = Math.abs(a - b);
    return delta < EPSILON;
  }
}
