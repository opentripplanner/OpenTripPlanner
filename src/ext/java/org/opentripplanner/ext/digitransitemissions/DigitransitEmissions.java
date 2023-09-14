package org.opentripplanner.ext.digitransitemissions;

import java.util.Objects;

public record DigitransitEmissions(double avg, int passengerAvg) {
  public DigitransitEmissions {
    Objects.requireNonNull(avg);
    Objects.requireNonNull(passengerAvg);
  }

  public double getEmissionsPerPassenger() {
    if (this.passengerAvg <= 1) {
      return this.avg;
    }
    return this.avg / this.passengerAvg;
  }
}
