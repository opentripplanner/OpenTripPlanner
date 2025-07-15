/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import java.util.Objects;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.tostring.ValueObjectToStringBuilder;

public final class ShapePoint implements Comparable<ShapePoint> {

  private static final double MISSING_VALUE = -999;

  private final int sequence;

  private final double lat;

  private final double lon;

  private final double distTraveled;

  public ShapePoint(int sequence, double lat, double lon, @Nullable Double distTraveled) {
    this.sequence = sequence;
    this.lat = DoubleUtils.requireInRange(lat, -90, 90, "latitude");
    this.lon = DoubleUtils.requireInRange(lon, -180, 180, "longitude");
    this.distTraveled = Objects.requireNonNullElse(distTraveled, MISSING_VALUE);
  }

  public int sequence() {
    return sequence;
  }

  public boolean isDistTraveledSet() {
    return distTraveled != MISSING_VALUE;
  }

  /**
   * @return the distance traveled along the shape path. If no distance was specified, the value is
   * undefined. Check first with {@link #isDistTraveledSet()}
   */
  public double distTraveled() {
    return distTraveled;
  }

  public double lat() {
    return lat;
  }

  public double lon() {
    return lon;
  }

  @Override
  public int hashCode() {
    return Objects.hash(lat, lon, sequence);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ShapePoint that = (ShapePoint) o;
    return (
      sequence == that.sequence &&
      Double.compare(lat, that.lat) == 0 &&
      Double.compare(lon, that.lon) == 0 &&
      Double.compare(distTraveled, that.distTraveled) == 0
    );
  }

  public boolean sameCoordinates(ShapePoint that) {
    return (Double.compare(this.lat, that.lat) == 0 && Double.compare(this.lon, that.lon) == 0);
  }

  @Override
  public String toString() {
    var s = ValueObjectToStringBuilder.of().addNum(sequence).addCoordinate(lat, lon);
    if (distTraveled != MISSING_VALUE) {
      s.addText(" dist=").addNum(distTraveled);
    }
    return s.toString();
  }

  @Override
  public int compareTo(ShapePoint o) {
    return this.sequence() - o.sequence();
  }

  public Coordinate coordinate() {
    return new Coordinate(lon, lat);
  }
}
