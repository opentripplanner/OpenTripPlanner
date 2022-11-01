package org.opentripplanner.ext.traveltime;

import org.opentripplanner.ext.traveltime.geometry.ZMetric;

public class IsolineMetric implements ZMetric<WTWD> {

  @Override
  public int cut(WTWD zA, WTWD zB, WTWD z0) {
    double t0 = z0.wTime / z0.w;
    double tA = zA.d > z0.d ? Double.POSITIVE_INFINITY : zA.wTime / zA.w;
    double tB = zB.d > z0.d ? Double.POSITIVE_INFINITY : zB.wTime / zB.w;
    if (tA < t0 && t0 <= tB) return 1;
    if (tB < t0 && t0 <= tA) return -1;
    return 0;
  }

  @Override
  public double interpolate(WTWD zA, WTWD zB, WTWD z0) {
    if (zA.d > z0.d || zB.d > z0.d) {
      if (zA.d > z0.d && zB.d > z0.d) {
        throw new AssertionError("dA > d0 && dB > d0");
      }
      // Interpolate on d
      return zA.d == zB.d ? 0.5 : (z0.d - zA.d) / (zB.d - zA.d);
    } else {
      // Interpolate on t
      double tA = zA.wTime / zA.w;
      double tB = zB.wTime / zB.w;
      double t0 = z0.wTime / z0.w;
      return tA == tB ? 0.5 : (t0 - tA) / (tB - tA);
    }
  }
}
