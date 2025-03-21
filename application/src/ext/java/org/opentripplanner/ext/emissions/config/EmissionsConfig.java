package org.opentripplanner.ext.emissions.config;

import static org.opentripplanner.ext.emissions.model.EmissionViechleParameters.CAR_DEFAULTS;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_5;

import org.opentripplanner.ext.emissions.model.EmissionParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * This class is responsible for mapping emissions configuration into emissions parameters.
 */
public class EmissionsConfig {

  public static EmissionParameters mapEmissionsConfig(String parameterName, NodeAdapter root) {
    var c = root
      .of(parameterName)
      .since(V2_5)
      .summary("Emissions configuration.")
      .description(
        """
        By specifying the average CO₂ emissions of a car in grams per kilometer as well as
        the average number of passengers in a car the program is able to to perform emission
        calculations for car travel.
        """
      )
      .asObject();

    return EmissionParameters.of()
      .withCar(car -> {
        car
          .withCarAvgCo2PerKm(
            c
              .of("carAvgCo2PerKm")
              .since(V2_5)
              .summary("The average CO₂ emissions of a car in grams per kilometer.")
              .asInt(CAR_DEFAULTS.avgCo2PerKm())
          )
          .withCarAvgOccupancy(
            c
              .of("carAvgOccupancy")
              .since(V2_5)
              .summary("The average number of passengers in a car.")
              .asDouble(CAR_DEFAULTS.avgOccupancy())
          );
      })
      .build();
  }
}
