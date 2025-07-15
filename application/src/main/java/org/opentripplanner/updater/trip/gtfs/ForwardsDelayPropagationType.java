package org.opentripplanner.updater.trip.gtfs;

import org.opentripplanner.framework.doc.DocumentedEnum;

public enum ForwardsDelayPropagationType implements DocumentedEnum<ForwardsDelayPropagationType> {
  NONE,
  DEFAULT;

  @Override
  public String typeDescription() {
    return "How forwards propagation should be handled.";
  }

  @Override
  public String enumValueDescription() {
    return switch (this) {
      case NONE -> """
      Do not propagate delays forwards. Reject real-time updates if not all arrival / departure times
      are specified until the end of the trip.

      Note that this will also reject all updates containing NO_DATA, or all updates containing
      SKIPPED stops without a time provided. Only use this value when you can guarantee that the
      real-time feed contains all departure and arrival times for all future stops, including
      SKIPPED stops.
      """;
      case DEFAULT -> """
      Default value. Propagate delays forwards for stops without arrival / departure times given.

      For NO_DATA stops, the scheduled time is used unless a previous delay fouls the scheduled time
      at the stop, in such case the minimum amount of delay is propagated to make the times
      non-decreasing.

      For SKIPPED stops without time given, interpolate the estimated time using the ratio between
      scheduled and real times from the previous to the next stop.
      """;
    };
  }
}
