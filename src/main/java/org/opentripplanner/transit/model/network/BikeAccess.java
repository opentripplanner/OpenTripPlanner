package org.opentripplanner.transit.model.network;

/**
 * GTFS codes:
 * 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes NOT allowed
 */
public enum BikeAccess {
  UNKNOWN,
  NOT_ALLOWED,
  ALLOWED,
}
