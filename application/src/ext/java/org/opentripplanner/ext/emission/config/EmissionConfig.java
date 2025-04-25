package org.opentripplanner.ext.emission.config;

import static org.opentripplanner.ext.emission.parameters.EmissionViechleParameters.CAR_DEFAULTS;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_5;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_8;

import java.util.List;
import org.opentripplanner.ext.emission.parameters.EmissionFeedParameters;
import org.opentripplanner.ext.emission.parameters.EmissionParameters;
import org.opentripplanner.ext.emission.parameters.EmissionViechleParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * This class is responsible for mapping emissions configuration into emissions parameters.
 */
public class EmissionConfig {

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
    return EmissionParameters.of().addFeeds(mapFeeds(c)).withCar(mapCar(c)).build();
  }

  private static EmissionViechleParameters mapCar(NodeAdapter c) {
    return new EmissionViechleParameters(
      c
        .of("carAvgCo2PerKm")
        .since(V2_5)
        .summary("The average CO₂ emissions of a car in grams per kilometer.")
        .asGram(CAR_DEFAULTS.avgCo2PerKm()),
      c
        .of("carAvgOccupancy")
        .since(V2_5)
        .summary("The average number of passengers in a car.")
        .asDouble(CAR_DEFAULTS.avgOccupancy())
    );
  }

  private static List<EmissionFeedParameters> mapFeeds(NodeAdapter c) {
    return c
      .of("feeds")
      .since(V2_8)
      .summary("List of emisstion feeds.")
      .asObjects(List.of(), EmissionConfig::mapFeed);
  }

  private static EmissionFeedParameters mapFeed(NodeAdapter c) {
    return new EmissionFeedParameters(
      c
        .of("feedId")
        .since(V2_8)
        .summary("Specify the feed id to use for matching transit ids in the emission input data.")
        .asString(),
      c.of("source").since(V2_8).summary("Specify the feed source url.").asUri()
    );
  }
}
