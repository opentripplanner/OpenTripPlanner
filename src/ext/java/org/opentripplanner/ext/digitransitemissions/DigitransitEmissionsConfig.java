package org.opentripplanner.ext.digitransitemissions;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * This class is responsible for mapping Digitransit emissions configuration into Digitransit emissions parameters.
 */
public class DigitransitEmissionsConfig {

  private int carAvgCo2PerKm;
  private double carAvgOccupancy;

  public DigitransitEmissionsConfig(String parameterName, NodeAdapter root) {
    var c = root
      .of(parameterName)
      .summary("Digitransit emissions configuration.")
      .description(
        """
        By specifying a URL to fetch emissions data, the program gains access to carbon dioxide (CO₂)
        emissions associated with transportation modes. This data is then used
        to perform emission calculations for public transport modes and car travel.
        """
      )
      .asObject();

    this.carAvgCo2PerKm =
      c
        .of("carAvgCo2PerKm")
        .summary("The average CO₂ emissions of a car in grams per kilometer.")
        .asInt(170);

    this.carAvgOccupancy =
      c.of("carAvgOccupancy").summary("The average number of passengers in a car.").asDouble(1.3);
  }

  public int getCarAvgCo2PerKm() {
    return carAvgCo2PerKm;
  }

  public double getCarAvgOccupancy() {
    return carAvgOccupancy;
  }
}
