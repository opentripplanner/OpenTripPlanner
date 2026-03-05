package org.opentripplanner.api.model.geometry;

import java.util.Objects;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.PolylineEncoder;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A list of coordinates encoded as a string, the length (number of coordinates) and the distance.
 * <p>
 * See <a href="http://code.google.com/apis/maps/documentation/polylinealgorithm.html">Encoded
 * polyline algorithm format</a>
 *
 * The attributes are lazy initialized to avoid unnecessary computation in case only the
 * line-string or the distance is requested, or none.
 *
 * THIS CLASS IS NOT THREAD-SAFE.
 */
public final class EncodedPolyline {

  private static final int NOT_SET = -1;
  private final Geometry geometry;

  private String points = null;
  private int length = NOT_SET;
  private int distance_m = NOT_SET;

  private EncodedPolyline(Geometry geometry) {
    this.geometry = geometry;
  }

  public static EncodedPolyline of(Geometry geometry) {
    return new EncodedPolyline(geometry);
  }

  public String points() {
    calculateLineString();
    return points;
  }

  public int length() {
    calculateLineString();
    return length;
  }

  public int distanceInMeters() {
    calculateDistance();
    return distance_m;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EncodedPolyline that = (EncodedPolyline) o;
    return length() == that.length() && Objects.equals(points(), that.points());
  }

  @Override
  public int hashCode() {
    return Objects.hash(points(), length());
  }

  @Override
  public String toString() {
    return ToStringBuilder.of("EncodedPolyline")
      .addNum("length", length())
      .addObj("points", points())
      .toString();
  }

  private void calculateLineString() {
    if (length == NOT_SET) {
      var line = PolylineEncoder.encodeGeometry(geometry);
      this.points = line.points();
      this.length = line.length();
    }
  }

  private void calculateDistance() {
    if (distance_m == NOT_SET) {
      distance_m = (int) GeometryUtils.sumDistances(geometry.getCoordinates());
    }
  }
}
