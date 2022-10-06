package org.opentripplanner.standalone.config.feed;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import org.opentripplanner.graph_builder.module.ned.parameter.DemExtractConfig;
import org.opentripplanner.graph_builder.module.ned.parameter.DemExtractConfigBuilder;
import org.opentripplanner.graph_builder.module.ned.parameter.DemExtractsConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * This class is responsible for mapping DEM(Elevation data) configuration into DEM parameters.
 */
public class DemConfig {

  public static DemExtractsConfig mapDemConfig(NodeAdapter root, String parameterName) {
    return new DemExtractsConfig(
      root
        .of(parameterName)
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .withDescription(/*TODO DOC*/"TODO")
        .asObjects(DemConfig::mapDemExtract)
    );
  }

  private static DemExtractConfig mapDemExtract(NodeAdapter config) {
    return new DemExtractConfigBuilder()
      .withSource(
        config.of("source").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asUri()
      )
      .withElevationUnitMultiplier(
        config
          .of("elevationUnitMultiplier")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .asDoubleOptional()
          .orElse(null)
      )
      .build();
  }
}
