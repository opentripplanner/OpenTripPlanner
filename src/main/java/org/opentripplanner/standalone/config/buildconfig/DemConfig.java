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
        .summary("TODO")
        .description(/*TODO DOC*/"TODO")
        .asObjects(DemConfig::mapDemExtract)
    );
  }

  private static DemExtractParameters mapDemExtract(NodeAdapter config) {
    return new DemExtractParametersBuilder()
      .withSource(config.of("source").since(NA).summary("TODO").asUri())
      .withElevationUnitMultiplier(
        config
          .of("elevationUnitMultiplier")
          .since(NA)
          .summary("TODO")
          .asDoubleOptional()
          .orElse(null)
      )
      .build();
  }
}
