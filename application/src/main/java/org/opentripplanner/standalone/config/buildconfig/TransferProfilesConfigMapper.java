package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_10;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.routerequest.RouteRequestConfig;
import org.opentripplanner.transit.transfer.regular.api.MaxDurationsPreference;
import org.opentripplanner.transit.transfer.regular.api.TransferProfileType;
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
        profile is used, mirroring the implicit default of the old `transferRequests` config. The
        max duration of the transfers is set with `defaultMaxDuration`, and can be overridden per
        profile type with `maxDurations`. The old `maxTransferDuration` is not used.
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

    var defaultMaxDurations = MaxDurationsPreference.of(
      transfers
        .of("defaultMaxDuration")
        .since(V2_10)
        .summary("The default max duration of the transfers, for all profile types.")
        .description(
          """
          How far from a stop the transfers are searched when the transfers are generated. It is
          used by all profile types that do not have an entry in `maxDurations`.
          """
        )
        .asDuration(MaxDurationsPreference.DEFAULT.maxDuration())
    );

    return new TransferProfilesParameters<>(walk, defaultMaxDurations, mapMaxDurations(transfers));
  }

  /**
   * One max duration configuration per profile type, it replaces the default for the type. The
   * type is the name of the {@link TransferProfileType} in lower case.
   */
  private static Map<TransferProfileType, MaxDurationsPreference> mapMaxDurations(
    NodeAdapter transfers
  ) {
    var node = transfers
      .of("maxDurations")
      .since(V2_10)
      .summary("The max durations of the transfers, by profile type.")
      .description(
        """
        One configuration per profile type. It is shared by all the profiles of the type, and it
        replaces `defaultMaxDuration` for the type. Only the type `walk` is supported for now.
        """
      )
      .asObject();
    var result = new EnumMap<TransferProfileType, MaxDurationsPreference>(
      TransferProfileType.class
    );
    for (var type : TransferProfileType.values()) {
      var key = type.name().toLowerCase();
      var entries = node
        .of(key)
        .since(V2_10)
        .summary("The max durations of the " + key + " transfers.")
        .asObjects(TransferProfilesConfigMapper::mapMaxDuration);
      if (entries.size() > 1) {
        throw new OtpAppException(
          "Only one max duration is supported for the transfer profile type '" +
            key +
            "', found " +
            entries.size() +
            "."
        );
      }
      if (!entries.isEmpty()) {
        result.put(type, MaxDurationsPreference.of(entries.getFirst()));
      }
    }
    return result;
  }

  private static Duration mapMaxDuration(NodeAdapter entry) {
    if (entry.exist("allowedModes")) {
      throw new OtpAppException(
        "'allowedModes' is not supported yet in the transfer 'maxDurations' in " +
          entry.contextPath()
      );
    }
    return entry
      .of("maxDuration")
      .since(V2_10)
      .summary("The longest transfer, measured with the user preferences of the profile.")
      .asDuration();
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
