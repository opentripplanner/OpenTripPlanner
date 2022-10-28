package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.graph_builder.module.ned.parameter.DemExtractParameters;
import org.opentripplanner.graph_builder.module.ned.parameter.DemExtractParametersBuilder;
import org.opentripplanner.graph_builder.module.ned.parameter.DemExtractParametersList;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * This class is responsible for mapping DEM(Elevation data) configuration into DEM parameters.
 */
public class DemConfig {

  public static DemExtractParametersList mapDemConfig(NodeAdapter root, String parameterName) {
    return new DemExtractParametersList(
      root
        .of(parameterName)
        .since(NA)
        .summary("Specify parameters for DEM extracts.")
        .description(
          "If not specified OTP will fall back to auto-detection based on the directory provided on the command line."
        )
        .asObjects(DemConfig::mapDemExtract)
    );
  }

  private static DemExtractParameters mapDemExtract(NodeAdapter config) {
    return new DemExtractParametersBuilder()
      .withSource(
        config.of("source").since(NA).summary("The unique URI pointing to the data file.").asUri()
      )
      .withElevationUnitMultiplier(
        config
          .of("elevationUnitMultiplier")
          .since(NA)
          .summary("Specify a multiplier to convert elevation units from source to meters.")
          .asDoubleOptional()
          .orElse(null)
      )
      .build();
  }
}
