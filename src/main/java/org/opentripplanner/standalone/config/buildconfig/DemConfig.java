package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;

import org.opentripplanner.graph_builder.module.ned.parameter.DemExtractParameters;
import org.opentripplanner.graph_builder.module.ned.parameter.DemExtractParametersBuilder;
import org.opentripplanner.graph_builder.module.ned.parameter.DemExtractParametersList;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * This class is responsible for mapping DEM(Elevation data) configuration into DEM parameters.
 */
public class DemConfig {

  public static DemExtractParameters mapDemDefaultsConfig(NodeAdapter root, String parameterName) {
    var demDefaults = root
      .of(parameterName)
      .since(V2_3)
      .summary("Default properties for DEM extracts.")
      .asObject();
    return mapDefaultDemParameters(demDefaults);
  }

  public static DemExtractParametersList mapDemConfig(
    NodeAdapter root,
    String parameterName,
    DemExtractParameters defaults
  ) {
    return new DemExtractParametersList(
      root
        .of(parameterName)
        .since(V2_2)
        .summary("Specify parameters for DEM extracts.")
        .description(
          """
          The dem section allows you to override the default behavior of scanning for elevation
          files in the [base directory](Configuration.md#Base-Directory). You can specify data
          located outside the local filesystem (including cloud storage services) or at various
          different locations around the local filesystem.

          If not specified OTP will fall back to auto-detection based on the directory provided on
          the command line.
          """
        )
        .asObjects(nodeAdapter -> mapDemExtract(nodeAdapter, defaults))
    );
  }

  private static DemExtractParameters mapDemExtract(
    NodeAdapter config,
    DemExtractParameters defaults
  ) {
    return defaults
      .copyOf()
      .withSource(
        config.of("source").since(V2_2).summary("The unique URI pointing to the data file.").asUri()
      )
      .withElevationUnitMultiplier(
        config
          .of("elevationUnitMultiplier")
          .since(V2_2)
          .summary("Specify a multiplier to convert elevation units from source to meters.")
          .description(
            """
            Unit conversion multiplier for elevation values. No conversion needed if the elevation
            values are defined in meters in the source data. If, for example, decimetres are used
            in the source data, this should be set to 0.1. This overrides the value specified in
            `demDefaults`.
          """
          )
          .asDouble(defaults.elevationUnitMultiplier())
      )
      .build();
  }

  private static DemExtractParameters mapDefaultDemParameters(NodeAdapter defaults) {
    return new DemExtractParametersBuilder()
      .withElevationUnitMultiplier(
        defaults
          .of("elevationUnitMultiplier")
          .since(V2_3)
          .summary("Specify a multiplier to convert elevation units from source to meters.")
          .description(
            """
            Unit conversion multiplier for elevation values. No conversion needed if the elevation
            values are defined in meters in the source data. If, for example, decimetres are used
            in the source data, this should be set to 0.1.
            """
          )
          .asDouble(DemExtractParameters.DEFAULT_ELEVATION_UNIT_MULTIPLIER)
      )
      .build();
  }
}
