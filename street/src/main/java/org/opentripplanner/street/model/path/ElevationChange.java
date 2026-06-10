package org.opentripplanner.street.model.path;

/// A DTO for expressing the gained and lost elevation. Both values are non-negative.
public record ElevationChange(double elevationGainedMeters, double elevationLostMeters) {
  public ElevationChange {
    if (elevationGainedMeters < 0) {
      throw new IllegalArgumentException("ElevationGainedMeters must be non-negative");
    }
    if (elevationLostMeters < 0) {
      throw new IllegalArgumentException("ElevationLostMeters must be non-negative");
    }
  }
}
