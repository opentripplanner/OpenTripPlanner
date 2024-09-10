package org.opentripplanner.routing.api.request.preference;

import org.opentripplanner.framework.doc.DocumentedEnum;

/**
 * Opt-in features for {@link org.opentripplanner.routing.algorithm.mapping.RaptorPathToItineraryMapper}.
 */
public enum MappingFeature implements DocumentedEnum {
  /**
   * If a transfer starts and ends at the very same stop, should a zero-length transfer leg be
   * added to the itinerary?
   */
  TRANSFER_LEG_ON_SAME_STOP;

  @Override
  public String typeDescription() {
    return "Feature of the mapping of the internal data structures into the final itinerary that are output by the APIs.";
  }

  @Override
  public String enumValueDescription() {
    return switch (this) {
      case TRANSFER_LEG_ON_SAME_STOP -> "Inserts a zero distance walking leg when the transfer starts and ends at the exact same stop.";
    };
  }
}
