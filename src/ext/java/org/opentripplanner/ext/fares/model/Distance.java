package org.opentripplanner.ext.fares.model;

public class Distance {

  private double distance;
  private boolean isKilometers;

  public Distance(double value, boolean isKilometers) {
    this.distance = value;
    this.isKilometers = isKilometers;
  }

  public static Distance ofMeters(double value) {
    return new Distance(value, false);
  }

  public static Distance ofKilometers(double value) {
    return new Distance(value, true);
  }

  public void toKilometers() {
    if (!this.isKilometers) {
      this.distance /= 1000;
      this.isKilometers = true;
    }
  }

  public void toMeters() {
    if (this.isKilometers) {
      this.distance *= 1000;
      this.isKilometers = false;
    }
  }

  public boolean isAbove(Distance otherDistance) {
    this.toMeters();
    otherDistance.toMeters();
    return this.distance > otherDistance.distance;
  }
}
