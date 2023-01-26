package org.opentripplanner.street.search;

import java.util.EnumSet;

public enum TraverseMode {
  WALK,
  BICYCLE,
  SCOOTER,
  CAR,
  FLEX;

  private static final EnumSet<TraverseMode> STREET_MODES = EnumSet.of(WALK, BICYCLE, SCOOTER, CAR);

  public boolean isOnStreetNonTransit() {
    return STREET_MODES.contains(this);
  }

  public boolean isDriving() {
    return this == CAR;
  }

  public boolean isCycling() {
    return this == BICYCLE || this == SCOOTER;
  }

  public boolean isWalking() {
    return this == WALK;
  }
}
