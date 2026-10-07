package org.opentripplanner.street.api.path;

/// A DTO for expressing the gained and lost elevation. Both values are non-negative.
public record ElevationChange(double elevationGainedMeters, double elevationLostMeters) {
  public static final ElevationChange ZERO = new ElevationChange(0, 0);

  public ElevationChange {
    if (elevationGainedMeters < 0) {
      throw new IllegalArgumentException("ElevationGainedMeters must be non-negative");
    }
    if (elevationLostMeters < 0) {
      throw new IllegalArgumentException("ElevationLostMeters must be non-negative");
    }
  }

  public ElevationChange plus(ElevationChange other) {
    return new ElevationChange(
      elevationGainedMeters + other.elevationGainedMeters,
      elevationLostMeters + other.elevationLostMeters
    );
  }
}
