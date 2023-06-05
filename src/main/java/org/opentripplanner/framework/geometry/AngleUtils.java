package org.opentripplanner.framework.geometry;

public class AngleUtils {
  public static byte calculateAngle(double lastAngle) {
    return (byte) Math.round(lastAngle * 128 / Math.PI + 128);
  }
}
