package org.opentripplanner.service.vehiclerental.model;

public enum ReturnPolicy {
  /**
   * The network has not specified which vehicle types can be returned. According to the GBFS spec,
   * this means that any vehicle type of that network can be returned.
   */
  ANY_TYPE,
  /**
   * Only specific vehicle types can be returned.
   */
  SPECIFIC_TYPES,
}
