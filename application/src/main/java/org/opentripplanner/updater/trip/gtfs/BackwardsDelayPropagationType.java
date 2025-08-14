package org.opentripplanner.updater.trip.gtfs;

import org.opentripplanner.framework.doc.DocumentedEnum;

/**
 * The backwards delay propagation type for a GTFS RT trip updater.
 */
public enum BackwardsDelayPropagationType implements DocumentedEnum<BackwardsDelayPropagationType> {
  NONE,
  REQUIRED_NO_DATA,
  REQUIRED,
  ALWAYS;

  @Override
  public String typeDescription() {
    return "How backwards propagation should be handled.";
  }

  @Override
  public String enumValueDescription() {
    return switch (this) {
      case NONE -> """
      Do not propagate delays backwards. Reject real-time updates if the times are not specified
      from the beginning of the trip.
      """;
      case REQUIRED_NO_DATA -> """
      Default value. Only propagates delays backwards when it is required to ensure that the times
          are increasing, and it sets the NO_DATA flag on the stops so these automatically updated times
          are not exposed through APIs.
      """;
      case REQUIRED -> """
      Only propagates delays backwards when it is required to ensure that the times are increasing.
          The updated times are exposed through APIs.
      """;
      case ALWAYS -> """
      Propagates delays backwards on stops with no estimates regardless if it's required or not.
          The updated times are exposed through APIs.
      """;
    };
  }
}
