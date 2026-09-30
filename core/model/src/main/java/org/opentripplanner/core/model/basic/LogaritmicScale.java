package org.opentripplanner.core.model.basic;

final class LogaritmicScale {

  private final double offset;
  private final double lnMax;

  public LogaritmicScale(double min, double max) {
    if (min >= max) {
      throw new IllegalArgumentException("min must be less than max");
    }
    this.offset = 1.0 - min;
    this.lnMax = Math.log(max + offset);
  }

  final float normalizedDistance(double a, double b) {
    double u = Math.log(a + offset);
    double v = Math.log(b + offset);
    return (float) (Math.abs(u - v) / lnMax);
  }
}
