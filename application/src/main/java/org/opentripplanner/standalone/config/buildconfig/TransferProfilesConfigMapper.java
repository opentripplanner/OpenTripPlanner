package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_10;

import java.util.List;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.routerequest.RouteRequestConfig;
import org.opentripplanner.transit.transfer.regular.api.TransferProfilesParameters;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.WalkProfileParameters;

/**
 * Parses the new {@code transfers:} build-config block (see
 * {@code opentripplanner/OpenTripPlanner#7998}) into the {@link TransferProfilesParameters} of the
 * regular-transfer module. Entirely separate from {@link RegularTransferConfig}/
 * {@code transferRequests}, which {@code FlexTransferGenerator} keeps reading unchanged for FLEX.
 * <p>
 * Only {@code walk} profiles are supported for now - support for other profile types, inheritance
 * ({@code parent}) and a duration limit is deliberately deferred.
 */
public class TransferProfilesConfigMapper {

  public static TransferProfilesParameters<RouteRequest> map(NodeAdapter root) {
    var transfers = root
      .of("transfers")
      .since(V2_10)
      .summary("Transfer profiles for the raptor-data regular-transfer pipeline.")
      .description(
        """
        Replaces `transferRequests` for regular (non-FLEX) transfer generation. Only `walk`
        profiles are supported for now. If this block is omitted entirely, a single default `walk`
        profile is used, mirroring the implicit default of the old `transferRequests` config.
        """
      )
      .asObject();

    var profiles = transfers
      .of("profiles")
      .since(V2_10)
      .summary("The transfer profiles, by profile type.")
      .asObject();

    var walk = profiles
      .of("walk")
      .since(V2_10)
      .summary("The walk transfer profiles.")
      .description(
        "Each profile runs a discovery search of walk transfers with its own preferences. The " +
          "`userPreferences` of the profiles must be unique."
      )
      .asObjects(List.of(defaultWalkProfile()), TransferProfilesConfigMapper::mapWalkProfile);

    return new TransferProfilesParameters<>(walk);
  }

  /** The profile used if no profile is configured. */
  private static WalkProfileParameters<RouteRequest> defaultWalkProfile() {
    return new WalkProfileParameters<>(WalkPreferences.DEFAULT, RouteRequest.defaultValue());
  }

  private static WalkProfileParameters<RouteRequest> mapWalkProfile(NodeAdapter p) {
    var userPreferences = mapWalkPreferences(
      p
        .of("userPreferences")
        .since(V2_10)
        .summary("Street-search preferences used to discover and cost this profile's paths.")
        .asObject()
    );
    var routingRequest = RouteRequestConfig.mapRouteRequest(
      p
        .of("routingRequest")
        .since(V2_10)
        .summary("The routing request parameters the street search of this profile is based on.")
        .description(
          "Preferences in `userPreferences` override the equivalent parameters of the request."
        )
        .asObject(),
      RouteRequest.defaultValue()
    );
    return new WalkProfileParameters<>(userPreferences, routingRequest);
  }

  private static WalkPreferences mapWalkPreferences(NodeAdapter p) {
    var dft = WalkPreferences.of();
    var original = dft.original();
    dft.withSpeed(
      Speed.ofMetersPerSecond(
        p
          .of("speed")
          .since(V2_10)
          .summary("Walking speed, in meters per second.")
          .asDouble(original.speed().toMetersPerSecond())
      )
    );
    dft.withReluctance(
      Reluctance.of(
        p
          .of("reluctance")
          .since(V2_10)
          .summary("A multiplier for the cost of walking.")
          .asDouble(original.reluctance().value())
      )
    );
    dft.withStairs(
      p
        .of("stairs")
        .since(V2_10)
        .summary("Whether stairs are allowed, discouraged, or forbidden for this profile.")
        .asEnum(original.stairs())
    );
    return dft.build();
  }
}
