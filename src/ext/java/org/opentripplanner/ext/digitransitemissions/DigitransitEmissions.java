package org.opentripplanner.ext.digitransitemissions;

import java.util.Objects;

public record DigitransitEmissions(
  double avgCo2PerVehiclePerMeter,
  double avgPassengerCount,
  double emissionsPerPassengerPerMeter
) {
  public DigitransitEmissions {
    Objects.requireNonNull(avgCo2PerVehiclePerMeter);
    Objects.requireNonNull(avgPassengerCount);
    Objects.requireNonNull(emissionsPerPassengerPerMeter);
  }

  public static DigitransitEmissions newDigitransitEmissions(
    double avgCo2PerVehiclePerMeter,
    double avgPassengerCount
  ) {
    return new DigitransitEmissions(
      avgCo2PerVehiclePerMeter,
      avgPassengerCount,
      calculateEmissionsPerPassengerPerMeter(avgCo2PerVehiclePerMeter, avgPassengerCount)
    );
  }

  private static double calculateEmissionsPerPassengerPerMeter(
    double avgCo2PerVehiclePerMeter,
    double avgPassengerCount
  ) {
    if (avgPassengerCount <= 1) {
      return avgCo2PerVehiclePerMeter;
    }
    return avgCo2PerVehiclePerMeter / avgPassengerCount;
  }

  public double getEmissionsPerPassenger() {
    return this.emissionsPerPassengerPerMeter;
  }
}
