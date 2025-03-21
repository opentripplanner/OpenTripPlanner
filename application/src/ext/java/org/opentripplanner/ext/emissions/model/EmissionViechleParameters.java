package org.opentripplanner.ext.emissions.model;

import org.opentripplanner.utils.tostring.ToStringBuilder;

public record EmissionViechleParameters(int avgCo2PerKm, double avgOccupancy) {
  private static final int CAR_AVG_CO2_PER_KM = 170;
  private static final double CAR_AVG_OCCUPANCY = 1.3;

  public static final EmissionViechleParameters CAR_DEFAULTS = new EmissionViechleParameters(
    CAR_AVG_CO2_PER_KM,
    CAR_AVG_OCCUPANCY
  );

  public Builder copyOf() {
    return new Builder(this);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(EmissionViechleParameters.class)
      .addNum("carAvgCo2PerKm", avgCo2PerKm)
      .addNum("carAvgOccupancy", avgOccupancy)
      .toString();
  }

  public static class Builder {

    private int carAvgCo2PerKm;
    private double carAvgOccupancy;

    public Builder(EmissionViechleParameters origin) {
      this.carAvgCo2PerKm = origin.avgCo2PerKm;
      this.carAvgOccupancy = origin.avgOccupancy;
    }

    public Builder withCarAvgCo2PerKm(int carAvgCo2PerKm) {
      this.carAvgCo2PerKm = carAvgCo2PerKm;
      return this;
    }

    public Builder withCarAvgOccupancy(double carAvgOccupancy) {
      this.carAvgOccupancy = carAvgOccupancy;
      return this;
    }

    public EmissionViechleParameters build() {
      return new EmissionViechleParameters(carAvgCo2PerKm, carAvgOccupancy);
    }
  }
}
