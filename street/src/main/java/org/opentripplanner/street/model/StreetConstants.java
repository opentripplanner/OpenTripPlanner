package org.opentripplanner.street.model;

/**
 * This class holds constant values related to streets. If a value is only accessed from one place,
 * it's better to store it there instead of here.
 */
public class StreetConstants {

  /**
   * Default car speed that is used when max car speed has not been (yet) determined from the OSM
   * data. Unit is m/s and value equals to 144 km/h.
   */
  public static final float DEFAULT_MAX_CAR_SPEED = 40f;
  public static final int DEFAULT_MAX_AREA_NODES = 200;
}
