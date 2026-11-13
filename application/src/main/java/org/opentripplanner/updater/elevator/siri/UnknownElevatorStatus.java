package org.opentripplanner.updater.elevator.siri;

import org.opentripplanner.core.model.doc.DocumentedEnum;

/** How an elevator whose status the feed does not know is treated. */
public enum UnknownElevatorStatus implements DocumentedEnum<UnknownElevatorStatus> {
  IN_SERVICE,
  OUT_OF_SERVICE,
  KEEP_PREVIOUS;

  @Override
  public String typeDescription() {
    return "How an elevator with an unknown status is treated.";
  }

  @Override
  public String enumValueDescription() {
    return switch (this) {
      case IN_SERVICE -> "The elevator is in service. This is the default.";
      case OUT_OF_SERVICE -> "The elevator is out of service, and is not used.";
      case KEEP_PREVIOUS -> """
      The elevator keeps the last known status from the feed. If there is none, it is in service.
      """;
    };
  }
}
