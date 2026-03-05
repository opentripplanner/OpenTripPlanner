package org.opentripplanner.ext.carpooling;

import org.opentripplanner.street.geometry.WgsCoordinate;

/**
 * Shared test coordinates and constants for carpooling tests.
 * Uses Oslo area coordinates for realistic geographic testing.
 */
public class CarpoolTestCoordinates {

  // Base coordinates (Oslo area)
  public static final WgsCoordinate OSLO_CENTER = new WgsCoordinate(59.9139, 10.7522);
  // ~2.5km east of center
  public static final WgsCoordinate OSLO_EAST = new WgsCoordinate(59.9149, 10.7922);
  // ~3.3km north of center
  public static final WgsCoordinate OSLO_NORTH = new WgsCoordinate(59.9439, 10.7522);
  // ~3.3km south of center
  public static final WgsCoordinate OSLO_SOUTH = new WgsCoordinate(59.8839, 10.7522);
  // ~2.5km west of center
  public static final WgsCoordinate OSLO_WEST = new WgsCoordinate(59.9139, 10.7122);

  // Coordinates for testing routes around obstacles (e.g., lake)
  public static final WgsCoordinate LAKE_NORTH = new WgsCoordinate(59.9439, 10.7522);
  public static final WgsCoordinate LAKE_EAST = new WgsCoordinate(59.9239, 10.7922);
  public static final WgsCoordinate LAKE_SOUTH = new WgsCoordinate(59.9039, 10.7522);
  public static final WgsCoordinate LAKE_WEST = new WgsCoordinate(59.9239, 10.7122);

  // Intermediate points for testing
  public static final WgsCoordinate OSLO_MIDPOINT_NORTH = new WgsCoordinate(59.9289, 10.7522);
  public static final WgsCoordinate OSLO_NORTHEAST = new WgsCoordinate(59.9439, 10.7922);
  public static final WgsCoordinate OSLO_NORTHWEST = new WgsCoordinate(59.9439, 10.7122);

  private CarpoolTestCoordinates() {
    // Utility class
  }
}
