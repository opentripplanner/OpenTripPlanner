package org.opentripplanner.street.search.request;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Represent the period, on which rental vehicle should be available in direct car street routing.
 */
public final class RentalPeriod {

  private final Instant start;

  private final Instant end;

  public RentalPeriod(Instant start, Instant end) {
    this.start = Objects.requireNonNull(start);
    this.end = Objects.requireNonNull(end);
  }

  public Instant start() {
    return start;
  }

  public Instant end() {
    return end;
  }

  public static RentalPeriod createFromLatestArrivalTime(Instant time, Duration rentalDuration) {
    return new RentalPeriod(time.minus(rentalDuration), time);
  }

  public static RentalPeriod createFromEarliestDepartureTime(
    Instant time,
    Duration rentalDuration
  ) {
    return new RentalPeriod(time, time.plus(rentalDuration));
  }
}
