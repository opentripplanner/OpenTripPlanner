package org.opentripplanner.transit.model.network;

/**
 * This represents the state of whether bikes are allowed on board trips (or routes).
 * If the state is unknown, bikes are not allowed.
 * <p>
 * GTFS codes:
 * 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes NOT allowed
 */
public enum BikeAccess {
  UNKNOWN,
  NOT_ALLOWED,
  ALLOWED,
}
