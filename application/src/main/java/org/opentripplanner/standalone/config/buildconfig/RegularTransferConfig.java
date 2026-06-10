package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_7;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_9;

import java.util.EnumMap;
import java.util.List;
import org.opentripplanner.graph_builder.module.transfer.api.RegularTransferParameters;
import org.opentripplanner.graph_builder.module.transfer.api.TransferParametersForMode;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.framework.json.OtpVersion;
import org.opentripplanner.standalone.config.routerequest.RouteRequestConfig;
import org.opentripplanner.street.model.StreetMode;

public class RegularTransferConfig {

  public static RegularTransferParameters map(NodeAdapter root) {
    var builder = RegularTransferParameters.of();
    var dft = RegularTransferParameters.DEFAULT;

    builder.withMaxDuration(
      root
        .of("maxTransferDuration")
        .since(V2_1)
        .summary(
          "Transfers up to this duration with a mode-specific speed value will be pre-calculated and included in the Graph."
        )
        .asDuration(dft.maxDuration())
    );

    builder.withParametersForMode(
      root
        .of("transferParametersForMode")
        .since(V2_7)
        .summary("Configures mode-specific properties for transfer calculations.")
        .description(
          """
          This field enables configuring mode-specific parameters for transfer calculations.
          To configure mode-specific parameters, the modes should also be used in the `transferRequests` field in the build config.

          **Example**

          ```JSON
          // build-config.json
          {
            "transferParametersForMode": {
              "CAR": {
                "disableDefaultTransfers": true,
                "carsAllowedStopMaxTransferDuration": "3h"
              },
              "BIKE": {
                "maxTransferDuration": "30m",
                "carsAllowedStopMaxTransferDuration": "3h"
              }
            }
          }
          ```
          """
        )
        .asEnumMap(
          StreetMode.class,
          RegularTransferConfig::mapParametersForMode,
          new EnumMap<>(StreetMode.class)
        )
    );

    builder.withRequests(
      root
        .of("transferRequests")
        .since(OtpVersion.V2_1)
        .summary("Routing requests to use for pre-calculating stop-to-stop transfers.")
        .description(
          """
          It will use the street network if OSM data has already been loaded into the graph. Otherwise it
          will use straight-line distance between stops.

          If not set, the default behavior is to generate stop-to-stop transfers using the default request
          with street mode set to WALK. Use this to change the default or specify more than one way to
          transfer.

          **Example**

          ```JSON
          // build-config.json
          {
            "transferRequests": [
              { "modes": "WALK" },
              { "modes": "WALK", "wheelchairAccessibility": { "enabled": true }}
            ]
          }
          ```
          """
        )
        .asObjects(List.of(RouteRequest.defaultValue()), RouteRequestConfig::mapRouteRequest)
    );
    return builder.build();
  }

  private static TransferParametersForMode mapParametersForMode(NodeAdapter c) {
    TransferParametersForMode.Builder builder = new TransferParametersForMode.Builder();
    builder.withMaxDuration(
      c
        .of("maxTransferDuration")
        .summary("This overwrites the default `maxTransferDuration` for the given mode.")
        .since(V2_7)
        .asDuration(TransferParametersForMode.DEFAULT_MAX_DURATION)
    );
    builder.withCarsAllowedStopMaxDuration(
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
        .asDuration(TransferParametersForMode.DEFAULT_CARS_ALLOWED_STOP_MAX_DURATION)
    );
    builder.withBikesAllowedStopMaxDuration(
      c
        .of("bikesAllowedStopMaxTransferDuration")
        .summary(
          "This is used for specifying a `maxTransferDuration` value to use with transfers between stops which are visited by trips that allow bikes."
        )
        .description(
          """
          This parameter configures additional transfers to be calculated for the specified mode only between stops that have trips with bikes.
          The transfers are calculated for the mode in a range based on the given duration.
          By default, these transfers are not calculated unless specified for a mode with this field.

          When compared to walking, using a bike can cover larger distances within the same duration specified in the `maxTransferDuration` field.
          This can lead to large amounts of transfers calculated between stops that do not require bike transfers between them.
          This in turn can lead to a large increase in memory for the stored graph, depending on the data used in the graph.

          For bikes, using this parameter in conjunction with `disableDefaultTransfers` allows calculating transfers only between stops
          which have trips which allow carrying bikes. This avoids storing and calculating transfers which are never used so the transit search
          can be faster compared to the default transfers.
          """
        )
        .since(V2_9)
        .asDuration(TransferParametersForMode.DEFAULT_BIKES_ALLOWED_STOP_MAX_DURATION)
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
          calculates transfers between stops that have trips with cars, or `bikesAllowedStopMaxTransferDuration` field for bikes.
          For example, when using the `carsAllowedStopMaxTransferDuration` field with cars, the default transfers can be redundant.
          """
        )
        .since(V2_7)
        .asBoolean(TransferParametersForMode.DEFAULT_DISABLE_DEFAULT_TRANSFERS)
    );
    return builder.build();
  }
}
