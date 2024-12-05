package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_7;

import org.opentripplanner.graph_builder.module.TransferParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class TransferParametersMapper {

  public static TransferParameters map(NodeAdapter c) {
    TransferParameters.Builder builder = new TransferParameters.Builder();
    builder.withMaxTransferDuration(
      c
        .of("maxTransferDuration")
        .summary("This overwrites the `maxTransferDuration` for the given mode.")
        .since(V2_7)
        .asDuration(TransferParameters.DEFAULT_MAX_TRANSFER_DURATION)
    );
    builder.withCarsAllowedStopMaxTransferDuration(
      c
        .of("carsAllowedStopMaxTransferDuration")
        .summary(
          "This is used for specifying a `maxTransferDuration` value to use with transfers between stops that have trips with cars."
        )
        .description(
          """
This parameter configures additional transfers to be calculated for the specified mode only between stops that have trips with cars.
The transfers are calculated for the mode in a range based on the given duration.
By default, these transfers are not calculated for the specified mode.
"""
        )
        .since(V2_7)
        .asDuration(TransferParameters.DEFAULT_CARS_ALLOWED_STOP_MAX_TRANSFER_DURATION)
    );
    builder.withDisableDefaultTransfers(
      c
        .of("disableDefaultTransfers")
        .summary("This disables default transfer calculations.")
        .since(V2_7)
        .asBoolean(TransferParameters.DEFAULT_DISABLE_DEFAULT_TRANSFERS)
    );
    return builder.build();
  }
}
