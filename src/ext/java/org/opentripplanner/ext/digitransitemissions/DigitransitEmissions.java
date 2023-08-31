package org.opentripplanner.ext.digitransitemissions;

import java.util.Objects;

public record DigitransitEmissions(double avg, int passengerAvg) {
  public DigitransitEmissions {
    Objects.requireNonNull(avg);
    Objects.requireNonNull(passengerAvg);
  }

  public double getEmissionsPerPassenger() {
    return this.avg / this.passengerAvg;
  }
}
