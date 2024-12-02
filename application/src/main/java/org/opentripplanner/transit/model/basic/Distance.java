package org.opentripplanner.transit.model.basic;

import java.util.Objects;
import org.opentripplanner.utils.tostring.ValueObjectToStringBuilder;

public class Distance {

  private static final int METERS_PER_KM = 1000;
  private final double meters;

  /** Returns a Distance object representing the given number of meters */
  public Distance(double value) {
    this.meters = value;
  }

  /** Returns a Distance object representing the given number of meters */
  public static Distance ofMeters(double value) {
    return new Distance(value);
  }

  /** Returns a Distance object representing the given number of kilometers */
  public static Distance ofKilometers(double value) {
    return new Distance(value * METERS_PER_KM);
  }

  /** Returns the distance in meters */
  public double toMeters() {
    return this.meters;
  }

  /**
   * Is this distance greater than the one passed in
   */
  public boolean greaterThan(Distance distance) {
    return this.meters > distance.meters;
  }

  /**
   * Is this distance less than the one passed in
   */
  public boolean lessThan(Distance distance) {
    return this.meters < distance.meters;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof Distance distance) {
      return distance.meters == this.meters;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(meters, 31);
  }

  @Override
  public String toString() {
    if (meters < METERS_PER_KM) {
      return ValueObjectToStringBuilder.of().addNum(meters, "m").toString();
    } else {
      return ValueObjectToStringBuilder.of().addNum(meters / 1000, "km").toString();
    }
  }
}
