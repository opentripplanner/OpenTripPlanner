package org.opentripplanner.ext.digitransitemissions;

import java.util.Objects;

public record DigitransitEmissions(double avgCo2PerVehiclePerKm, int avgPassengerCount) {
  public DigitransitEmissions {
    Objects.requireNonNull(avgCo2PerVehiclePerKm);
    Objects.requireNonNull(avgPassengerCount);
  }

  public double getEmissionsPerPassenger() {
    if (this.avgPassengerCount <= 1) {
      return this.avgCo2PerVehiclePerKm;
    }
    return this.avgCo2PerVehiclePerKm / this.avgPassengerCount;
  }
}
