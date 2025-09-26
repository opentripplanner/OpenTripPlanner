package org.opentripplanner.street.search.request;

import java.time.Instant;

public class RentalPeriodBuilder {

  private Instant rentalStartTime;
  private Instant rentalEndTime;

  public RentalPeriodBuilder setRentalStartTime(Instant rentalStartTime) {
    this.rentalStartTime = rentalStartTime;
    return this;
  }

  public RentalPeriodBuilder setRentalEndTime(Instant rentalEndTime) {
    this.rentalEndTime = rentalEndTime;
    return this;
  }

  public Instant rentalStartTime() {
    return rentalStartTime;
  }

  public Instant rentalEndTime() {
    return rentalEndTime;
  }

  public RentalPeriod build() {
    return new RentalPeriod(this);
  }
}
