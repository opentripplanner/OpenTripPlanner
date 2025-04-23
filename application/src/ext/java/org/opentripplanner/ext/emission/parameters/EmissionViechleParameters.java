package org.opentripplanner.ext.emission.parameters;

import org.opentripplanner.framework.model.Gram;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public record EmissionViechleParameters(Gram avgCo2PerKm, double avgOccupancy) {
  private static final Gram CAR_AVG_CO2_PER_KM = Gram.of(170);
  private static final double CAR_AVG_OCCUPANCY = 1.3;

  public static final EmissionViechleParameters CAR_DEFAULTS = new EmissionViechleParameters(
    CAR_AVG_CO2_PER_KM,
    CAR_AVG_OCCUPANCY
  );

  public EmissionViechleParameters {
    DoubleUtils.requireInRange(avgCo2PerKm.asDouble(), 0.0, 2000.0, "avgCo2PerKm");
    DoubleUtils.requireInRange(avgOccupancy, 0.1, 20.0, "avgOccupancy");
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(EmissionViechleParameters.class)
      .addObj("carAvgCo2PerKm", avgCo2PerKm)
      .addNum("carAvgOccupancy", avgOccupancy)
      .toString();
  }
}
