package org.opentripplanner.ext.digitransitemissions;

import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * This class is responsible for mapping Digitransit emissions configuration into Digitransit emissions parameters.
 */
public class DigitransitEmissionsConfig {

  private int carAvgCo2;
  private double carAvgOccupancy;

  public DigitransitEmissionsConfig(String parameterName, NodeAdapter root) {
    var c = root
      .of(parameterName)
      .summary("Digitransit emissions configuration.")
      .description(
        """
        By specifying a URL to fetch emissions data, the program gains access to carbon dioxide (CO2)
        emissions associated with transportation modes. This data is then used
        to perform emission calculations for public transport modes and car travel.
        """
      )
      .asObject();

    this.carAvgCo2 = c.of("carAvgCo2").summary("The average CO2 emissions of a car.").asInt(170);

    this.carAvgOccupancy =
      c.of("carAvgOccupancy").summary("The average number of passengers in a car.").asDouble(1.3);
  }

  public int getCarAvgCo2() {
    return carAvgCo2;
  }

  public double getCarAvgOccupancy() {
    return carAvgOccupancy;
  }
}
