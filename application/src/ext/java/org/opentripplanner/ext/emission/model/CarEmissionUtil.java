package org.opentripplanner.ext.emission.model;

import org.opentripplanner.ext.emission.parameters.EmissionViechleParameters;
import org.opentripplanner.framework.model.Gram;

/**
 * This class is responsible of calculating car emission.
 */
public class CarEmissionUtil {

  /**
   * Calculate car CO2 car emission for one person per meter.
   */
  public static Gram calculateCarCo2EmissionPerMeterPerPerson(EmissionViechleParameters carConfig) {
    Gram carCo2PerKm = carConfig.avgCo2PerKm();
    double carAvgOccupancy = carConfig.avgOccupancy();
    Gram carCo2PerMeterPerPerson = carCo2PerKm.dividedBy(1000).dividedBy(carAvgOccupancy);
    return carCo2PerMeterPerPerson;
  }
}
