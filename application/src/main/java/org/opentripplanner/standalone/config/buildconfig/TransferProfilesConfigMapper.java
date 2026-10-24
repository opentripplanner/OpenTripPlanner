package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_10;

import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.routerequest.RouteRequestConfig;
import org.opentripplanner.transfer.streetadapter.RegularTransferPreferencesMapper;
import org.opentripplanner.transit.transfer.regular.api.MaxDurationsPreference;
import org.opentripplanner.transit.transfer.regular.api.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.TransferProfilesParameters;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.WalkProfileParameters;
import org.opentripplanner.transit.transfer.regular.configure.RegularTransferFactory;

/**
 * Parses the {@code transfers:} build-config block, see {@code doc/user/TransferConfiguration.md},
 * into the {@link TransferProfilesParameters} of the regular-transfer module. Entirely separate
 * from {@link RegularTransferConfig}/{@code transferRequests}, which {@code FlexTransferGenerator}
 * keeps reading unchanged for FLEX.
 * <p>
 * The configuration of a profile type is a block named after the type, for example {@code walk},
 * with the profiles and the max durations of the type. Only {@code walk} is supported for now -
 * {@code parent}, {@code warmup}, and the other types are deliberately deferred.
 */
public class TransferProfilesConfigMapper {

  /** The id of the profile used if no profile is configured. */
  static final String DEFAULT_WALK_PROFILE_ID = "walk";

  public static TransferProfilesParameters<RouteRequest> map(NodeAdapter root) {
    var transfers = root
      .of("transfers")
      .since(V2_10)
      .summary("Transfer profiles for the raptor-data regular-transfer pipeline.")
      .description(
        """
        Replaces `transferRequests` for regular (non-FLEX) transfer generation. The configuration of
        a profile type is
        a block named after the type, with its `profiles` and `maxDurations`. Only the type `walk`
        is supported for now. If this block is omitted, a single default `walk` profile is used,
        mirroring the implicit default of the old `transferRequests` config. The max duration of
        the transfers is set with `defaultMaxDuration`, and can be overridden for a profile type
        with `maxDurations`. The old `maxTransferDuration` is not used.
        """
      )
      .asObject();

    var defaultMaxDurations = MaxDurationsPreference.of(
      transfers
        .of("defaultMaxDuration")
        .since(V2_10)
        .summary("The default max duration of the transfers, for all profile types.")
        .description(
          """
          How far from a stop the transfers are searched when the transfers are generated. It is
          used by all profile types that do not have `maxDurations`.
          """
        )
        .asDuration(MaxDurationsPreference.DEFAULT.maxDuration())
    );

    var walk = transfers
      .of("walk")
      .since(V2_10)
      .summary("The configuration of the walk transfers.")
      .description(
        """
        The `profiles`, each a discovery search of walk transfers with its own preferences, and the
        `maxDurations`, one configuration shared by all the walk profiles.
        """
      )
      .asObject();

    var profiles = walk
      .of("profiles")
      .since(V2_10)
      .summary("The walk transfer profiles.")
      .description(
        """
        Each profile needs an `id`, unique among all the profiles. The `userPreferences` of the
        profiles must be unique. A profile without `userPreferences` gets them from its
        `routingRequest`. If no profile is configured, a default profile with the id `walk` is used.
        """
      )
      .asObjects(List.of(), TransferProfilesConfigMapper::mapWalkProfile);
    if (profiles.isEmpty()) {
      profiles = List.of(defaultWalkProfile());
    }
    if (walk.exist("warmup")) {
      throw new OtpAppException(
        "'warmup' is not supported yet in the transfer configuration, in " + walk.contextPath()
      );
    }

    var maxDurations = new EnumMap<TransferProfileType, MaxDurationsPreference>(
      TransferProfileType.class
    );
    var walkMaxDurations = mapMaxDurations(walk, TransferProfileType.WALK);
    if (walkMaxDurations != null) {
      maxDurations.put(TransferProfileType.WALK, walkMaxDurations);
    }

    return createParameters(profiles, defaultMaxDurations, maxDurations);
  }

  /**
   * Create and validate the parameters. A configuration error is reported like the other
   * configuration errors, when the configuration is read.
   */
  private static TransferProfilesParameters<RouteRequest> createParameters(
    List<WalkProfileParameters<RouteRequest>> profiles,
    MaxDurationsPreference defaultMaxDurations,
    Map<TransferProfileType, MaxDurationsPreference> maxDurations
  ) {
    try {
      var parameters = new TransferProfilesParameters<>(
        profiles,
        defaultMaxDurations,
        maxDurations
      );
      RegularTransferFactory.validate(parameters, new RegularTransferPreferencesMapper());
      return parameters;
    } catch (IllegalArgumentException e) {
      throw new OtpAppException("Invalid transfer configuration: " + e.getMessage());
    }
  }

  /**
   * The max durations of a profile type: one configuration, it replaces the default for the type.
   * {@code null} if the type has none.
   */
  @Nullable
  private static MaxDurationsPreference mapMaxDurations(
    NodeAdapter typeNode,
    TransferProfileType type
  ) {
    var entries = typeNode
      .of("maxDurations")
      .since(V2_10)
      .summary("The max durations of the " + type.name().toLowerCase() + " transfers.")
      .description(
        """
        One configuration for the profile type, shared by all its profiles. It replaces
        `defaultMaxDuration` for the type.
        """
      )
      .asObjects(TransferProfilesConfigMapper::mapMaxDuration);
    if (entries.size() > 1) {
      throw new OtpAppException(
        "Only one max duration is supported for the transfer profile type '" +
          type.name().toLowerCase() +
          "', found " +
          entries.size() +
          "."
      );
    }
    return entries.isEmpty() ? null : MaxDurationsPreference.of(entries.getFirst());
  }

  private static Duration mapMaxDuration(NodeAdapter entry) {
    if (entry.exist("allowedModes")) {
      throw new OtpAppException(
        "'allowedModes' is not supported yet in the transfer 'maxDurations', in " +
          entry.contextPath()
      );
    }
    return entry
      .of("maxDuration")
      .since(V2_10)
      .summary("The longest transfer, measured with the user preferences of the profile.")
      .asDuration();
  }

  /** The profile used if no profile is configured: its preferences are the ones of the request. */
  private static WalkProfileParameters<RouteRequest> defaultWalkProfile() {
    return new WalkProfileParameters<>(DEFAULT_WALK_PROFILE_ID, null, RouteRequest.defaultValue());
  }

  private static WalkProfileParameters<RouteRequest> mapWalkProfile(NodeAdapter p) {
    if (p.exist("parent")) {
      throw new OtpAppException(
        "'parent' is not supported yet in the transfer profiles, in " + p.contextPath()
      );
    }
    var id = p
      .of("id")
      .since(V2_10)
      .summary("The id of the profile.")
      .description(
        """
        Identifies the profile in the logs and the issues reported. It must be unique among all the
        profiles. Any string is allowed.
        """
      )
      .asString();
    WalkPreferences userPreferences = null;
    if (p.exist("userPreferences")) {
      userPreferences = mapWalkPreferences(
        p
          .of("userPreferences")
          .since(V2_10)
          .summary("Street-search preferences used to discover and cost this profile's paths.")
          .description(
            "Optional. If it is not set, the preferences of the `routingRequest` are used."
          )
          .asObject()
      );
    }
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
    return new WalkProfileParameters<>(id, userPreferences, routingRequest);
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
