package org.opentripplanner.transit.model.network;

/**
 * This represents the state of whether cars are allowed on board trips.
 * <p>
 * GTFS codes:
 * 0 = unknown / unspecified, 1 = cars allowed, 2 = cars NOT allowed
 */
public enum CarAccess {
  UNKNOWN,
  NOT_ALLOWED,
  ALLOWED,
}
