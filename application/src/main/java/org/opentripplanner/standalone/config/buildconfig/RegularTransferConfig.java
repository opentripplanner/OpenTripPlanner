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

    builder.withIncludeStops(
      root
        .of("stopsWithRegularTransfers")
        .since(V2_9)
        .summary(
          "Stops that should always have regular transfers computed, even without scheduled trips."
        )
        .description(
          """
          List of stop IDs for which regular transfers are always pre-computed during graph build,
          even if the stop has no scheduled trips. Remember to include _feedId_ like this
          `"RB:NSR:Quay:102541"`.

          This is useful for stops that are unused in static transit data, but may be visited by
          real-time updates (e.g. a platform that a train can be re-routed to at runtime). Without
          this configuration, stops with no scheduled trips are excluded from transfer pre-computation
          and become unreachable islands when a real-time update routes a trip to them.

          Note! This parameter should be replaced with an automatic update to regular transfers
          based on real-time updates.
          """
        )
        .experimentalFeature()
        .asFeedScopedIds(List.of())
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
        .description(
          """
          A car or a bike can cover a much larger distance than walking within the same duration.
          Reusing this value would reduce the search radius, calculating fewer transfers and
          decreases the graph memory usage.

          If it isn't known which stops actually allow cars/bikes, combine a lower value here with
          `carsAllowedStopMaxTransferDuration` or `bikesAllowedStopMaxTransferDuration` as a
          compromise: this bounds memory usage for stops in general, while the allowed-stop field
          still supplies a longer range for the stops it is known to be needed for.
          """
        )
        .since(V2_7)
        .asDuration(TransferParametersForMode.DEFAULT_MAX_DURATION)
    );
    builder.withCarsAllowedStopMaxDuration(
      c
        .of("carsAllowedStopMaxTransferDuration")
        .summary(
          """
          This is used for specifying a `maxTransferDuration` value to use with transfers between
          stops which are visited by trips that allow cars.
          """
        )
        .description(
          """
          Configures a separate `maxTransferDuration` for the given mode, used only for transfers
          between stops visited by trips that allow cars (e.g. car ferries), instead of the given
          mode's `maxTransferDuration`.

          This can also be configured for other modes. For example, for bikes, this can enable
          transfers between ferry stops that would otherwise be out of range, since car ferries
          usually also allow bikes. This is useful for bike routes using ferries near the Turku
          archipelago in Finland, for example.
          """
        )
        .since(V2_7)
        .asDuration(TransferParametersForMode.DEFAULT_CARS_ALLOWED_STOP_MAX_DURATION)
    );
    builder.withBikesAllowedStopMaxDuration(
      c
        .of("bikesAllowedStopMaxTransferDuration")
        .summary(
          """
          This is used for specifying a `maxTransferDuration` value to use with transfers between
          stops which are visited by trips that allow bikes.
          """
        )
        .description(
          """
          Configures a separate `maxTransferDuration` for the given mode, used only for transfers
          between stops visited by trips that allow bikes, instead of the given mode's
          `maxTransferDuration`.

          In combination with the mode's `maxTransferDuration` you can include transfers for bikes
          between all stops in a smaller radius, and use a larger radius for transfers between
          stops where bikes are explicit allowed.
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
          By default, transfers are calculated between all stop pairs within the given mode's
          `maxTransferDuration`. This parameter disables that default calculation for the mode.

          This is used together with `carsAllowedStopMaxTransferDuration` or
          `bikesAllowedStopMaxTransferDuration`, so that only the restricted, relevant transfers are
          calculated.
          """
        )
        .since(V2_7)
        .asBoolean(TransferParametersForMode.DEFAULT_DISABLE_DEFAULT_TRANSFERS)
    );
    return builder.build();
  }
}
