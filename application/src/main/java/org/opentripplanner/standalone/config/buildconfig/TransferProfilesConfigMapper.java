package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_10;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
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
import org.opentripplanner.transit.transfer.regular.api.maxduration.MaxDurationsPreference;
import org.opentripplanner.transit.transfer.regular.api.maxduration.PlainMaxDuration;
import org.opentripplanner.transit.transfer.regular.api.parameters.DeduplicationLimit;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileParameters;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfilesParameters;
import org.opentripplanner.transit.transfer.regular.api.parameters.WarmupParameters;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.BicyclePreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.CarPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WheelchairPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WithSpeed;
import org.opentripplanner.transit.transfer.regular.api.preferences.WithStairs;
import org.opentripplanner.transit.transfer.regular.api.ways.StreetSegmentTypeUsage;
import org.opentripplanner.transit.transfer.regular.configure.RegularTransferFactory;
import org.opentripplanner.utils.collection.ParentOrder;

/**
 * Parses the {@code transfers:} build-config block, see {@code doc/user/TransferConfiguration.md},
 * into the {@link TransferProfilesParameters} of the regular-transfer module. Entirely separate
 * from {@link RegularTransferConfig}/{@code transferRequests}, which {@code FlexTransferGenerator}
 * keeps reading unchanged for FLEX.
 * <p>
 * The configuration of a profile type is a block named after the type, for example {@code walk},
 * with the profiles, the max durations and the warmup entries of the type. The types {@code walk},
 * {@code wheelchair}, {@code bicycle} and {@code car} are supported for now, the scooter is
 * deliberately deferred.
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
        a profile type is a block named after the type, with its `profiles`, its `maxDurations` and
        its `warmup` entries. The types `walk`, `wheelchair`, `bicycle` and `car` are supported for
        now. If no profile is configured, a single default `walk` profile is used, mirroring the
        implicit default of the old `transferRequests` config. The max duration of the transfers is
        set with `defaultMaxDuration`, and can be overridden for a profile type with
        `maxDurations`. The old `maxTransferDuration` is not used.
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

    var deduplicationLimit = mapDeduplicationLimit(transfers);

    var typeBlocks = new EnumMap<TransferProfileType, NodeAdapter>(TransferProfileType.class);
    var maxDurations = new EnumMap<TransferProfileType, MaxDurationsPreference>(
      TransferProfileType.class
    );
    for (var type : TransferProfileType.values()) {
      var block = mapTypeBlock(transfers, type);
      typeBlocks.put(type, block);
      var typeMaxDurations = mapMaxDurations(block, type);
      if (typeMaxDurations != null) {
        maxDurations.put(type, typeMaxDurations);
      }
    }

    var mappedProfiles = mapProfiles(typeBlocks);
    var profiles = mappedProfiles.profiles();
    var warmup = mapWarmup(typeBlocks, mappedProfiles.preferencesById());

    return createParameters(
      profiles,
      defaultMaxDurations,
      maxDurations,
      deduplicationLimit,
      warmup
    );
  }

  /** The block of a profile type, for example {@code walk}. An empty block if it is not set. */
  private static NodeAdapter mapTypeBlock(NodeAdapter transfers, TransferProfileType type) {
    var name = typeName(type);
    return transfers
      .of(name)
      .since(V2_10)
      .summary("The configuration of the " + name + " transfers.")
      .description(
        """
        The `profiles`, each a discovery search of transfers with its own preferences, the
        `maxDurations`, one configuration shared by all the profiles of the type, and the `warmup`
        entries.
        """
      )
      .asObject();
  }

  private static String typeName(TransferProfileType type) {
    return type.name().toLowerCase();
  }

  /**
   * Create and validate the parameters. A configuration error is reported like the other
   * configuration errors, when the configuration is read.
   */
  private static TransferProfilesParameters<RouteRequest> createParameters(
    List<TransferProfileParameters<RouteRequest>> profiles,
    PlainMaxDuration defaultMaxDurations,
    Map<TransferProfileType, MaxDurationsPreference> maxDurations,
    DeduplicationLimit deduplicationLimit,
    List<WarmupParameters> warmup
  ) {
    try {
      var parameters = new TransferProfilesParameters<>(
        profiles,
        defaultMaxDurations,
        maxDurations,
        deduplicationLimit,
        warmup
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
    return entries.isEmpty() ? null : MaxDurationsPreference.of(type, entries.getFirst());
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
  private static TransferProfileParameters<RouteRequest> defaultWalkProfile() {
    return new TransferProfileParameters<>(
      DEFAULT_WALK_PROFILE_ID,
      TransferProfileType.WALK,
      null,
      RouteRequest.defaultValue()
    );
  }

  private static DeduplicationLimit mapDeduplicationLimit(NodeAdapter transfers) {
    var text = transfers
      .of("deduplicationLimit")
      .since(V2_10)
      .summary("How much more the path of a parent profile may cost, to be used by its children.")
      .description(
        """
        A profile with a `parent` finds its own paths, but if the path of the parent costs about the
        same for the profile, it uses the path of the parent instead of storing its own. This saves
        memory. The paths are equivalent if the difference in generalized cost is within this limit,
        a linear function of the cost `x` of the path of the parent, for example `0.005x + 30s`. The
        limit applies to cost only, not to duration. A report is logged when the graph is built.
        """
      )
      .asString(null);
    if (text == null) {
      return DeduplicationLimit.DEFAULT;
    }
    try {
      return DeduplicationLimitParser.parse(text);
    } catch (IllegalArgumentException e) {
      throw new OtpAppException("Invalid transfer configuration: " + e.getMessage());
    }
  }

  /** A profile as configured: its id and its parent are read first, to order the profiles. */
  private record ConfiguredProfile(
    String id,
    TransferProfileType type,
    @Nullable String parent,
    NodeAdapter node
  ) {}

  /** A profile with the values that are inherited by its children. */
  private record ResolvedProfile(
    RouteRequest request,
    AbstractUserPreferences<?> userPreferences
  ) {}

  /** The mapped profiles, and the user preferences of each profile, by id, after inheritance. */
  private record MappedProfiles(
    List<TransferProfileParameters<RouteRequest>> profiles,
    Map<String, AbstractUserPreferences<?>> preferencesById
  ) {}

  /**
   * Map the profiles of all the types. A profile inherits unset values from its parent, so the
   * parents are mapped first, whatever order they are declared in, and the parent can be of
   * another type. If no profile is configured, a default walk profile is used.
   */
  private static MappedProfiles mapProfiles(Map<TransferProfileType, NodeAdapter> typeBlocks) {
    var configured = new ArrayList<ConfiguredProfile>();
    typeBlocks.forEach((type, block) ->
      configured.addAll(
        block
          .of("profiles")
          .since(V2_10)
          .summary("The " + typeName(type) + " transfer profiles.")
          .description(
            """
            Each profile needs an `id`, unique among all the profiles. The `userPreferences` of the
            profiles must be unique. A profile without `userPreferences` gets them from its
            `routingRequest`. A profile with a `parent` inherits the `routingRequest` and the
            `userPreferences` of the parent, and the paths it has in common with the parent are
            stored once.
            """
          )
          .asObjects(List.of(), node -> readConfiguredProfile(node, type))
      )
    );
    if (configured.isEmpty()) {
      var profile = defaultWalkProfile();
      return new MappedProfiles(
        List.of(profile),
        Map.of(
          profile.id(),
          new RegularTransferPreferencesMapper().toUserPreferences(profile.routingRequest())
        )
      );
    }

    List<ConfiguredProfile> parentsFirst;
    try {
      parentsFirst = ParentOrder.parentsFirst(
        configured,
        ConfiguredProfile::id,
        ConfiguredProfile::parent
      );
    } catch (IllegalArgumentException e) {
      throw new OtpAppException("Invalid transfer configuration: " + e.getMessage());
    }

    var mapper = new RegularTransferPreferencesMapper();
    var resolved = new HashMap<String, ResolvedProfile>();
    var mapped = new HashMap<String, TransferProfileParameters<RouteRequest>>();
    for (var profile : parentsFirst) {
      var parent = profile.parent() == null ? null : resolved.get(profile.parent());
      var request = RouteRequestConfig.mapRouteRequest(
        profile
          .node()
          .of("routingRequest")
          .since(V2_10)
          .summary("The routing request parameters the street search of this profile is based on.")
          .description(
            "Preferences in `userPreferences` override the equivalent parameters of the request."
          )
          .asObject(),
        parent == null ? RouteRequest.defaultValue() : parent.request()
      );
      AbstractUserPreferences<?> own = null;
      if (profile.node().exist("userPreferences")) {
        own = mapPreferences(
          profile.type(),
          profile
            .node()
            .of("userPreferences")
            .since(V2_10)
            .summary("Street-search preferences used to discover and cost this profile's paths.")
            .description(
              "Optional. If it is not set, the preferences of the `routingRequest` are used."
            )
            .asObject(),
          parent == null ? null : parent.userPreferences()
        );
      }
      var effective = own != null ? own : mapper.toUserPreferences(profile.type(), request);
      // The children inherit the request with the preferences of this profile applied
      resolved.put(
        profile.id(),
        new ResolvedProfile(mapper.toRequest(effective, request), effective)
      );
      mapped.put(
        profile.id(),
        new TransferProfileParameters<>(
          profile.id(),
          profile.type(),
          profile.parent(),
          own,
          request
        )
      );
    }
    // Keep the declared order
    var preferencesById = new HashMap<String, AbstractUserPreferences<?>>();
    resolved.forEach((id, profile) -> preferencesById.put(id, profile.userPreferences()));
    return new MappedProfiles(
      configured
        .stream()
        .map(p -> mapped.get(p.id()))
        .toList(),
      preferencesById
    );
  }

  /**
   * Map the warmup entries of all the types. The preferences of an entry inherit the preferences
   * of its parent profile, so an entry only sets what is different.
   */
  private static List<WarmupParameters> mapWarmup(
    Map<TransferProfileType, NodeAdapter> typeBlocks,
    Map<String, AbstractUserPreferences<?>> preferencesById
  ) {
    var result = new ArrayList<WarmupParameters>();
    typeBlocks.forEach((type, block) ->
      result.addAll(
        block
          .of("warmup")
          .since(V2_10)
          .summary(
            "The user preferences to create the Raptor transfers for when the server starts."
          )
          .description(
            """
            Each entry needs an `id`, a `parent` and `userPreferences`. The `id` must be unique
            among all the profiles and warmup entries, and the `parent` is the `id` of a profile of
            the same type. The Raptor transfers for the `userPreferences` are created from the
            paths of the parent when the server starts, so the first request with these preferences
            does not wait for them. No paths are searched for or stored for a warmup entry. Use
            warmup for preferences that are common in the requests, but are not worth a profile of
            their own.
            """
          )
          .asObjects(List.of(), entry -> mapWarmupEntry(entry, type, preferencesById))
      )
    );
    return result;
  }

  private static WarmupParameters mapWarmupEntry(
    NodeAdapter entry,
    TransferProfileType type,
    Map<String, AbstractUserPreferences<?>> preferencesById
  ) {
    var id = entry
      .of("id")
      .since(V2_10)
      .summary("The id of the warmup entry.")
      .description("Identifies the entry in the logs. It must be unique among all the profiles.")
      .asString();
    var parent = entry
      .of("parent")
      .since(V2_10)
      .summary("The id of the profile the Raptor transfers are created from.")
      .asString();
    var parentPreferences = preferencesById.get(parent);
    if (parentPreferences == null) {
      throw new OtpAppException(
        "Invalid transfer configuration: The parent '" +
          parent +
          "' of the warmup '" +
          id +
          "' is not a profile"
      );
    }
    if (parentPreferences.type() != type) {
      throw new OtpAppException(
        "Invalid transfer configuration: The parent '" +
          parent +
          "' of the warmup '" +
          id +
          "' is a " +
          typeName(parentPreferences.type()) +
          " profile, the warmup is in the " +
          typeName(type) +
          " block"
      );
    }
    if (!entry.exist("userPreferences")) {
      throw new OtpAppException(
        "Invalid transfer configuration: The warmup '" + id + "' needs 'userPreferences'"
      );
    }
    var userPreferences = mapPreferences(
      type,
      entry
        .of("userPreferences")
        .since(V2_10)
        .summary("The user preferences to warm up.")
        .description("Unset values are the values of the parent.")
        .asObject(),
      parentPreferences
    );
    return new WarmupParameters(id, parent, userPreferences);
  }

  private static ConfiguredProfile readConfiguredProfile(NodeAdapter p, TransferProfileType type) {
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
    String parent = null;
    if (p.exist("parent")) {
      parent = p
        .of("parent")
        .since(V2_10)
        .summary("The id of the profile this profile is based on.")
        .description(
          """
          The profile inherits the `routingRequest` and the `userPreferences` of the parent. Values
          set on the profile override the inherited ones. The parent is generated first, and the
          paths of the profile that are equivalent to the paths of the parent are stored once, see
          `deduplicationLimit`. The parents must not form a cycle.
          """
        )
        .asString();
    }
    return new ConfiguredProfile(id, type, parent, p);
  }

  /**
   * Map the user preferences of a profile type. Unset values are the values of {@code parent}, the
   * preferences of the parent profile or the warmup, or else the defaults of the type. A parent of
   * another type gives the values the two types have in common.
   */
  private static AbstractUserPreferences<?> mapPreferences(
    TransferProfileType type,
    NodeAdapter p,
    @Nullable AbstractUserPreferences<?> parent
  ) {
    return switch (type) {
      case WALK -> mapWalkPreferences(p, WalkPreferences.inheritedFrom(parent));
      case WHEELCHAIR -> mapWheelchairPreferences(p, WheelchairPreferences.inheritedFrom(parent));
      case BICYCLE -> mapBicyclePreferences(p, BicyclePreferences.inheritedFrom(parent));
      case CAR -> mapCarPreferences(p, CarPreferences.inheritedFrom(parent));
    };
  }

  private static WalkPreferences mapWalkPreferences(NodeAdapter p, WalkPreferences base) {
    var builder = base.copyOf();
    builder.withSpeed(mapSpeed(p, "Walking speed, in meters per second.", base));
    builder.withReluctance(mapReluctance(p, "A multiplier for the cost of walking.", base));
    builder.withStairs(mapStairs(p, base));
    return builder.build();
  }

  private static WheelchairPreferences mapWheelchairPreferences(
    NodeAdapter p,
    WheelchairPreferences base
  ) {
    var builder = base.copyOf();
    builder.withSpeed(
      mapSpeed(p, "Speed of a traveler in a wheelchair, in meters per second.", base)
    );
    builder.withReluctance(
      mapReluctance(p, "A multiplier for the cost of traveling in a wheelchair.", base)
    );
    builder.withStairs(mapStairs(p, base));
    return builder.build();
  }

  private static BicyclePreferences mapBicyclePreferences(NodeAdapter p, BicyclePreferences base) {
    var builder = base.copyOf();
    builder.withSpeed(mapSpeed(p, "Bicycle speed, in meters per second.", base));
    builder.withReluctance(
      mapReluctance(p, "A multiplier for the cost of riding a bicycle.", base)
    );
    builder.withStairs(mapStairs(p, base));
    return builder.build();
  }

  private static CarPreferences mapCarPreferences(NodeAdapter p, CarPreferences base) {
    var builder = base.copyOf();
    builder.withReluctance(mapReluctance(p, "A multiplier for the cost of driving.", base));
    return builder.build();
  }

  private static <T extends AbstractUserPreferences<T> & WithSpeed> Speed mapSpeed(
    NodeAdapter p,
    String summary,
    T base
  ) {
    return Speed.ofMetersPerSecond(
      p.of("speed").since(V2_10).summary(summary).asDouble(base.speed().toMetersPerSecond())
    );
  }

  private static Reluctance mapReluctance(
    NodeAdapter p,
    String summary,
    AbstractUserPreferences<?> base
  ) {
    return Reluctance.of(
      p.of("reluctance").since(V2_10).summary(summary).asDouble(base.reluctance().value())
    );
  }

  private static <
    T extends AbstractUserPreferences<T> & WithStairs
  > StreetSegmentTypeUsage mapStairs(NodeAdapter p, T base) {
    return p
      .of("stairs")
      .since(V2_10)
      .summary("Whether stairs are allowed, discouraged, or forbidden for this profile.")
      .asEnum(base.stairs());
  }
}
