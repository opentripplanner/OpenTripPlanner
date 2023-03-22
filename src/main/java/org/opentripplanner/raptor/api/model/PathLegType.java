package org.opentripplanner.raptor.api.model;

/**
 * In Raptor each leg can be either access, transit, transfer or egress.
 *
 */
public enum PathLegType {
  /**
   * The access is always the first leg.
   */
  ACCESS,
  /**
   * Travelling on a public transport vehicle.
   */
  TRANSIT,
  /**
   * A transfer can not follow another transfer, and can only be connected to access and egress
   * if the access/egress has arrived/departed from the stop by a ride.
   */
  TRANSFER,
  /**
   * The egress is always the last leg.
   */
  EGRESS;

  public boolean is(PathLegType value) {
    return value == this;
  }

  public boolean not(PathLegType value) {
    return value != this;
  }
}
