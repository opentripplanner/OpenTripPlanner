package org.opentripplanner.street.geometry;

import org.locationtech.jts.geom.Coordinate;

public class TestCoordinates {

  public static final Coordinate BERLIN_TV_TOWER = of(52.5212, 13.4105);
  public static final Coordinate BERLIN_BRANDENBURG_GATE = of(52.51627, 13.37770);

  /**
   * Because it is a frequent mistake to swap x/y and longitude/latitude when
   * constructing JTS Coordinates, this static factory method makes is clear
   * which is which.
   */
  public static Coordinate of(double latitude, double longitude) {
    return new Coordinate(longitude, latitude);
  }
}
