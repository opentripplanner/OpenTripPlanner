package org.opentripplanner.transit.api.request;

/**
 * Controls whether canceled trip times are included in a {@link TripTimeOnDateRequest} result. A
 * trip time is considered canceled if the whole trip is canceled/replaced or the visit at the stop
 * has been skipped.
 */
public enum CancellationPolicy {
  /** Only return canceled trip times. */
  ONLY_CANCELLATIONS,
  /** Do not return any canceled trip times. */
  NO_CANCELLATIONS,
  /** Return canceled trip times in addition to the non-canceled ones. */
  INCLUDE_CANCELLATIONS;

  /**
   * Whether canceled trip times should be part of the result at all. This is {@code true} for both
   * {@link #ONLY_CANCELLATIONS} and {@link #INCLUDE_CANCELLATIONS}.
   */
  public boolean includesCancellations() {
    return this == ONLY_CANCELLATIONS || this == INCLUDE_CANCELLATIONS;
  }

  /**
   * Whether the result should be restricted to canceled trip times only.
   */
  public boolean onlyCancellations() {
    return this == ONLY_CANCELLATIONS;
  }
}
