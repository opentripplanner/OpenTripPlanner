package org.opentripplanner.street.search.request;

import java.time.Instant;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Represent the period, on which rental vehicle should be available in direct car street routing.
 */
public final class RentalPeriod {

  @Nonnull
  private final Instant rentalStartTime;

  @Nonnull
  private final Instant rentalEndTime;

  public RentalPeriod(RentalPeriodBuilder rentalPeriodBuilder) {
    this.rentalStartTime = Objects.requireNonNull(rentalPeriodBuilder.rentalStartTime());
    this.rentalEndTime = Objects.requireNonNull(rentalPeriodBuilder.rentalEndTime());
  }

  public Instant rentalStartTime() {
    return rentalStartTime;
  }

  public Instant rentalEndTime() {
    return rentalEndTime;
  }
}
