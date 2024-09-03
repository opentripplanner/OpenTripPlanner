package org.opentripplanner.transit.model.network;

/**
 * GTFS codes:
 * 0 = unknown / unspecified, 1 = cars allowed, 2 = cars NOT allowed
 */
public enum CarAccess {
  UNKNOWN,
  NOT_ALLOWED,
  ALLOWED,
}
