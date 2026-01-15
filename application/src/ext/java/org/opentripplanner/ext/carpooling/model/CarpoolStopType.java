package org.opentripplanner.ext.carpooling.model;

/**
 * The type of carpool stop operation.
 */
public enum CarpoolStopType {
  /** Only passengers can be picked up at this stop */
  PICKUP_ONLY,
  /** Only passengers can be dropped off at this stop */
  DROP_OFF_ONLY,
  /** Both pickup and drop-off are allowed */
  PICKUP_AND_DROP_OFF,
}
