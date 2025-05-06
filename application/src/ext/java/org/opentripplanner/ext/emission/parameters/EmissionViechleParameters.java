package org.opentripplanner.ext.emission.parameters;

import org.opentripplanner.utils.tostring.ToStringBuilder;

public record EmissionViechleParameters(int avgCo2PerKm, double avgOccupancy) {
  private static final int CAR_AVG_CO2_PER_KM = 170;
  private static final double CAR_AVG_OCCUPANCY = 1.3;

  public static final EmissionViechleParameters CAR_DEFAULTS = new EmissionViechleParameters(
    CAR_AVG_CO2_PER_KM,
    CAR_AVG_OCCUPANCY
  );

  @Override
  public String toString() {
    return ToStringBuilder.of(EmissionViechleParameters.class)
      .addNum("carAvgCo2PerKm", avgCo2PerKm)
      .addNum("carAvgOccupancy", avgOccupancy)
      .toString();
  }
}
