package org.opentripplanner.routing.vehicle_parking;

/**
 * The state of the vehicle parking. TEMPORARILY_CLOSED and CLOSED are distinct states so that they
 * may be represented differently to the user.
 */
public enum VehicleParkingState {
  OPERATIONAL,
  TEMPORARILY_CLOSED,
  CLOSED,
}
