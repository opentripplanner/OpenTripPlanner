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
        .summary("")
        .since(V2_7)
        .asDuration(TransferParameters.DEFAULT_MAX_TRANSFER_DURATION)
    );
    builder.withCarsAllowedStopMaxTransferDuration(
      c
        .of("carsAllowedStopMaxTransferDuration")
        .summary("")
        .since(V2_7)
        .asDuration(TransferParameters.DEFAULT_CARS_ALLOWED_STOP_MAX_TRANSFER_DURATION)
    );
    builder.withDisableDefaultTransfers(
      c
        .of("disableDefaultTransfers")
        .summary("")
        .since(V2_7)
        .asBoolean(TransferParameters.DEFAULT_DISABLE_DEFAULT_TRANSFERS)
    );
    return builder.build();
  }
}
