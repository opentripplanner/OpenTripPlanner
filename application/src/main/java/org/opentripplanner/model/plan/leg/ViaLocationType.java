package org.opentripplanner.model.plan.leg;

/**
 * Categorization for a via location.
 */
public enum ViaLocationType {
  /**
   * The location is visited physically by boarding or alighting a transit trip at a given stop, or
   * by traveling via requested coordinate location as part of a access, transfer, egress or direct
   * segment. Intermediate stops visited on-board do not count.
   */
  VISIT,
  /**
   * The via stop location must be visited as part of a transit trip as at the boarding stop, the
   * intermediate stop, or the alighting stop.
   */
  PASS_THROUGH,
}
