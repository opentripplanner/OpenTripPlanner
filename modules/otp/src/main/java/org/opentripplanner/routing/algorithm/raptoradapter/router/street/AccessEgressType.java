package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

/**
 * Access and Egress share many features, so instead of duplicating the logic we can make methods
 * generic and use this "type" explicit differentiate(and not use a boolean flag).
 */
public enum AccessEgressType {
  ACCESS,
  EGRESS;

  public boolean isAccess() {
    return this == ACCESS;
  }

  public boolean isEgress() {
    return this == EGRESS;
  }
}
