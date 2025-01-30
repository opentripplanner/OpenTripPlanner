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
        .summary("This overwrites the default `maxTransferDuration` for the given mode.")
        .since(V2_7)
        .asDuration(TransferParameters.DEFAULT_MAX_TRANSFER_DURATION)
    );
    builder.withCarsAllowedStopMaxTransferDuration(
      c
        .of("carsAllowedStopMaxTransferDuration")
        .summary(
          "This is used for specifying a `maxTransferDuration` value to use with transfers between stops which are visited by trips that allow cars."
        )
        .description(
          """
This parameter configures additional transfers to be calculated for the specified mode only between stops that have trips with cars.
The transfers are calculated for the mode in a range based on the given duration.
By default, these transfers are not calculated unless specified for a mode with this field.

Calculating transfers only between stops that have trips with cars can be useful with car ferries, for example.
Using transit with cars can only occur between certain stops.
These kinds of stops require support for loading cars into ferries, for example.
The default transfers are calculated based on a configurable range (configurable by using the `maxTransferDuration` field)
which limits transfers from stops to only be calculated to other stops that are in range.
When compared to walking, using a car can cover larger distances within the same duration specified in the `maxTransferDuration` field.
This can lead to large amounts of transfers calculated between stops that do not require car transfers between them.
This in turn can lead to a large increase in memory for the stored graph, depending on the data used in the graph.

For cars, using this parameter in conjunction with `disableDefaultTransfers` allows calculating transfers only between relevant stops.
For bikes, using this parameter can enable transfers between ferry stops that would normally not be in range.
In Finland this is useful for bike routes that use ferries near the Turku archipelago, for example.
"""
        )
        .since(V2_7)
        .asDuration(TransferParameters.DEFAULT_CARS_ALLOWED_STOP_MAX_TRANSFER_DURATION)
    );
    builder.withDisableDefaultTransfers(
      c
        .of("disableDefaultTransfers")
        .summary("This disables default transfer calculations.")
        .description(
          """
The default transfers are calculated based on a configurable range (configurable by using the `maxTransferDuration` field)
which limits transfers from stops to only be calculated to other stops that are in range.
This parameter disables these transfers.
A motivation to disable default transfers could be related to using the `carsAllowedStopMaxTransferDuration` field which only
calculates transfers between stops that have trips with cars.
For example, when using the `carsAllowedStopMaxTransferDuration` field with cars, the default transfers can be redundant.
"""
        )
        .since(V2_7)
        .asBoolean(TransferParameters.DEFAULT_DISABLE_DEFAULT_TRANSFERS)
    );
    return builder.build();
  }
}
