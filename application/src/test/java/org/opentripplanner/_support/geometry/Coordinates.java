package org.opentripplanner._support.geometry;

import org.locationtech.jts.geom.Coordinate;

public class Coordinates {

  public static final Coordinate BERLIN = of(52.5212, 13.4105);
  public static final Coordinate BERLIN_BRANDENBURG_GATE = of(52.51627, 13.37770);
  public static final Coordinate HAMBURG = of(53.5566, 10.0003);
  public static final Coordinate KONGSBERG_PLATFORM_1 = of(59.67216, 9.65107);
  public static final Coordinate BOSTON = of(42.36541, -71.06129);

  /**
   * Because it is a frequent mistake to swap x/y and longitude/latitude when
   * constructing JTS Coordinates, this static factory method makes is clear
   * which is which.
   */
  public static Coordinate of(double latitude, double longitude) {
    return new Coordinate(longitude, latitude);
  }
}
