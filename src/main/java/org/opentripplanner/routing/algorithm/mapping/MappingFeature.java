package org.opentripplanner.routing.algorithm.mapping;

/**
 * Opt-in features for {@link RaptorPathToItineraryMapper}.
 */
public enum MappingFeature {
  /**
   * If a transfer starts and ends at the very same stop, should a zero-length transfer leg be
   * added to the itinerary?
   */
  TRANSFER_LEG_ON_SAME_STOP,
}
