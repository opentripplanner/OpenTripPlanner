package org.opentripplanner.model;

/**
 * A StopLocation describes a place where a vehicle can be boarded or alighted, which is not
 * necessarily a marked stop, but can be of other shapes, such as a service area for flexible
 * transit. StopLocations are referred toin stop times.
 */
public interface StopLocation {

  /** The ID for the StopLocation */
  FeedScopedId getId();

  /** Name of the StopLocation, if provided */
  String getName();

  /** Public facing stop code (short text or number). */
  String getCode();

  /**
   * Representative location for the StopLocation. Can either be the actual location of the stop, or
   * the centroid of an area or line.
   */
  WgsCoordinate getCoordinate();
  
  boolean isLine();

  boolean isArea();
}
