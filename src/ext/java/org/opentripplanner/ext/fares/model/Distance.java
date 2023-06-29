package org.opentripplanner.ext.fares.model;

public class Distance {

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
    return new Distance(value * 1000);
  }

  /** Returns the distance in kilometers */
  public double toKilometers() {
    return this.meters / 1000;
  }

  /** Returns the distance in meters */
  public double toMeters() {
    return this.meters;
  }

  /** Returns whether this distance is greater that the given distance */
  public boolean isAbove(Distance otherDistance) {
    return this.toMeters() > otherDistance.toMeters();
  }

  /** Returns whether this distance is smaller than the given distance */
  public boolean isBelow(Distance otherDistance) {
    return this.toMeters() < otherDistance.toMeters();
  }
}
