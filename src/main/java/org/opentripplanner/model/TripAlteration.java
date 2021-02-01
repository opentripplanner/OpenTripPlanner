package org.opentripplanner.model;

public enum TripAlteration {
  cancellation,
  planned,
  extraJourney,
  replaced;

  public boolean isCanceledOrReplaced() {
    return this == cancellation || this == replaced;
  }
}
