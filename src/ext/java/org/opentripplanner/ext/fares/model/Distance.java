package org.opentripplanner.ext.fares.model;

public class Distance {

  private final double meters;

  public Distance(double value) {
    this.meters = value;
  }

  public static Distance ofMeters(double value) {
    return new Distance(value);
  }

  public static Distance ofKilometers(double value) {
    return new Distance(value);
  }

  public double toKilometers() {
    return this.meters / 1000;
  }

  public double toMeters() {
    return this.meters;
  }

  public boolean isAbove(Distance otherDistance) {
    return this.toMeters() > otherDistance.toMeters();
  }

  public boolean isBelow(Distance otherDistance) {
    return this.toMeters() < otherDistance.toMeters();
  }
}
