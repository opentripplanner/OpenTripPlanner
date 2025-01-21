package org.opentripplanner.transit.model.basic;

import org.opentripplanner.utils.tostring.ValueObjectToStringBuilder;

public class Distance {

  private static final int MILLIMETERS_PER_M = 1000;
  private static final int MILLIMETERS_PER_KM = 1000 * MILLIMETERS_PER_M;
  private final int millimeters;

  /** Returns a Distance object representing the given number of meters */
  private Distance(int distanceInMillimeters) {
    if (distanceInMillimeters < 0) {
      throw new IllegalArgumentException("Distance cannot be negative");
    }

    this.millimeters = distanceInMillimeters;
  }

  /** Returns a Distance object representing the given number of meters */
  public static Distance ofMeters(double value) throws IllegalArgumentException {
    return new Distance((int) (value * MILLIMETERS_PER_M));
  }

  /** Returns a Distance object representing the given number of kilometers */
  public static Distance ofKilometers(double value) {
    return new Distance((int) (value * MILLIMETERS_PER_KM));
  }

  /** Returns the distance in meters */
  public int toMeters() {
    double meters = (double) this.millimeters / (double) MILLIMETERS_PER_M;
    return (int) Math.round(meters);
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof Distance distance) {
      return distance.millimeters == this.millimeters;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(this.millimeters);
  }

  @Override
  public String toString() {
     if (millimeters > MILLIMETERS_PER_KM) {
      return ValueObjectToStringBuilder
        .of()
        .addNum((double) this.millimeters / (double) MILLIMETERS_PER_KM, "km")
        .toString();
    } else {
      return ValueObjectToStringBuilder
        .of()
        .addNum((double) this.millimeters / (double) MILLIMETERS_PER_M, "m")
        .toString();
    }
  }
}
