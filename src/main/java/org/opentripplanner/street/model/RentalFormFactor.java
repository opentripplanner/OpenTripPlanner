package org.opentripplanner.street.model;

import org.opentripplanner.street.search.TraverseMode;

public enum RentalFormFactor {
  BICYCLE(TraverseMode.BICYCLE),
  CARGO_BICYCLE(TraverseMode.BICYCLE),
  CAR(TraverseMode.CAR),
  MOPED(TraverseMode.BICYCLE),
  SCOOTER(TraverseMode.BICYCLE),
  SCOOTER_STANDING(TraverseMode.BICYCLE),
  SCOOTER_SEATED(TraverseMode.BICYCLE),
  OTHER(TraverseMode.BICYCLE);

  public final TraverseMode traverseMode;

  RentalFormFactor(TraverseMode traverseMode) {
    this.traverseMode = traverseMode;
  }
}
