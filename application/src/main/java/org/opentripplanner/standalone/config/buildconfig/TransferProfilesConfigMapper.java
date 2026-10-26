package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_10;

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
import org.opentripplanner.transit.transfer.regular.api.maxduration.BicycleMaxDurations;
import org.opentripplanner.transit.transfer.regular.api.maxduration.CarMaxDuration;
import org.opentripplanner.transit.transfer.regular.api.maxduration.MaxDurationsPreference;
import org.opentripplanner.transit.transfer.regular.api.maxduration.PlainMaxDuration;
import org.opentripplanner.transit.transfer.regular.api.parameters.DeduplicationLimit;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferParameters;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfile;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.parameters.WarmupParameters;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.BicyclePreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.BicycleWalk;
import org.opentripplanner.transit.transfer.regular.api.preferences.CarPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WheelchairPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WithSpeed;
import org.opentripplanner.transit.transfer.regular.configure.RegularTransferFactory;
import org.opentripplanner.utils.collection.ParentOrder;

/**
 * Parses the {@code transfers:} build-config block, see {@code doc/user/TransferConfiguration.md},
 * into the {@link TransferParameters} of the regular-transfer module. Entirely separate
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

  public static TransferParameters<RouteRequest> map(NodeAdapter root) {
    var transfers = root
      .of("transfers")
      .since(V2_10)
      .summary("Transfer profiles for the raptor-data regular-transfer pipeline.")
      .description(
        """
        Replaces `transferRequests` for regular (non-FLEX) transfer generation. The configuration of
        a profile type is a block named after the type, with its `profiles`, its `maxDuration` and
        its `warmup` entries. The types `walk`, `wheelchair`, `bicycle` and `car` are supported for
        now. If no profile is configured, a single default `walk` profile with a max duration of 30
        minutes is used, mirroring the implicit default of the old `transferRequests` config. Each
        profile type with profiles must set its `maxDuration`. The old `maxTransferDuration` is
        not used.
        """
      )
      .asObject();

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
    if (mappedProfiles.isDefaultWalkProfile()) {
      maxDurations.putIfAbsent(TransferProfileType.WALK, MaxDurationsPreference.DEFAULT);
    }

    return createParameters(profiles, maxDurations, deduplicationLimit, warmup);
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
        `maxDuration`, shared by all the profiles of the type, and the `warmup` entries.
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
  private static TransferParameters<RouteRequest> createParameters(
    List<TransferProfile<RouteRequest>> profiles,
    Map<TransferProfileType, MaxDurationsPreference> maxDurations,
    DeduplicationLimit deduplicationLimit,
    List<WarmupParameters> warmup
  ) {
    try {
      var parameters = new TransferParameters<>(profiles, maxDurations, deduplicationLimit, warmup);
      RegularTransferFactory.validate(parameters, new RegularTransferPreferencesMapper());
      return parameters;
    } catch (IllegalArgumentException e) {
      throw new OtpAppException("Invalid transfer configuration: " + e.getMessage());
    }
  }

  /**
   * The max durations of a profile type, shared by all its profiles. {@code null} if the type has
   * none, which is an error if the type has profiles.
   */
  @Nullable
  private static MaxDurationsPreference mapMaxDurations(
    NodeAdapter typeNode,
    TransferProfileType type
  ) {
    var name = typeName(type);
    var maxDuration = typeNode
      .of("maxDuration")
      .since(V2_10)
      .summary("The longest " + name + " transfer, measured with the user preferences.")
      .description(maxDurationDescription(type))
      .asDuration(null);

    if (type != TransferProfileType.BICYCLE) {
      rejectBicycleOnly(typeNode, "maxDurationAllowAllModes", name);
      rejectBicycleOnly(typeNode, "maxDurationAllowsCar", name);
      if (maxDuration == null) {
        return null;
      }
      return type == TransferProfileType.CAR
        ? new CarMaxDuration(maxDuration)
        : new PlainMaxDuration(maxDuration);
    }

    var allowAllModes = typeNode
      .of("maxDurationAllowAllModes")
      .since(V2_10)
      .summary("The longest bicycle transfer between any two stops.")
      .description(
        """
        A bicycle can cover a long distance, so this is usually short. It must be shorter than
        `maxDuration` and `maxDurationAllowsCar`. If it is not set, there are only transfers
        between the stops that allow bicycles or cars, as set by `maxDuration` and
        `maxDurationAllowsCar`.
        """
      )
      .asDuration(null);
    var allowsCar = typeNode
      .of("maxDurationAllowsCar")
      .since(V2_10)
      .summary("The longest bicycle transfer between stops that allow cars.")
      .description(
        """
        Used for the transfers between two stops that are visited by trips that allow cars, for
        example a car ferry, which a bicycle can use too. It must be longer than
        `maxDurationAllowAllModes`.
        """
      )
      .asDuration(null);
    if (maxDuration == null && allowAllModes == null && allowsCar == null) {
      return null;
    }
    try {
      return new BicycleMaxDurations(allowAllModes, maxDuration, allowsCar);
    } catch (IllegalArgumentException e) {
      throw new OtpAppException(
        "Invalid transfer configuration in " + typeNode.contextPath() + ": " + e.getMessage()
      );
    }
  }

  private static String maxDurationDescription(TransferProfileType type) {
    var description = """
    How far from a stop the transfers are searched when the transfers are generated. It is
    shared by all the profiles of the type, and is not inherited from another type or from a
    parent profile. Each type with profiles must set it.
    """;
    return switch (type) {
      case WALK, WHEELCHAIR -> description +
        "\nThere are transfers between all stops within the duration.\n";
      case BICYCLE -> description +
        "\nThere are transfers within the duration between the stops that allow bicycles.\n";
      case CAR -> description +
        """

        There are transfers within the duration between the stops that allow cars only, and no
        transfers from or to other stops. A car reaches a lot of stops in a short time, so a car
        search from all stops does not work, even for small deployments. If there are too many stops
        that allow cars, fix the data, or let the OTP developers know the use case.
        """;
    };
  }

  private static void rejectBicycleOnly(NodeAdapter typeNode, String parameter, String typeName) {
    if (typeNode.exist(parameter)) {
      throw new OtpAppException(
        "'" +
          parameter +
          "' is only supported for the transfer profile type 'bicycle', not '" +
          typeName +
          "', in " +
          typeNode.contextPath()
      );
    }
  }

  /** The profile used if no profile is configured: its preferences are the ones of the request. */
  private static TransferProfile<RouteRequest> defaultWalkProfile() {
    return new TransferProfile<>(
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
    List<TransferProfile<RouteRequest>> profiles,
    Map<String, AbstractUserPreferences<?>> preferencesById,
    boolean isDefaultWalkProfile
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
        ),
        true
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
    var mapped = new HashMap<String, TransferProfile<RouteRequest>>();
    for (var profile : parentsFirst) {
      // Only a parent of the same type is inherited from. A parent of another type is only used to
      // store the paths that are the same once, see the deduplication.
      var parent = profile.parent() == null ? null : resolved.get(profile.parent());
      var inherited =
        parent != null && parent.userPreferences().type() == profile.type() ? parent : null;
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
        inherited == null ? RouteRequest.defaultValue() : inherited.request()
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
          inherited == null
            ? mapper.toUserPreferences(profile.type(), request)
            : inherited.userPreferences()
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
        new TransferProfile<>(profile.id(), profile.type(), profile.parent(), own, request)
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
      preferencesById,
      false
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
          If the parent is a profile of the same type, the profile inherits the `routingRequest` and
          the `userPreferences` of the parent, and values set on the profile override the inherited
          ones. The root of a type gets the values that are not set from the request. The parent is
          generated first, and the paths of the profile that are equivalent to the paths of the
          parent are stored once, see `deduplicationLimit`. The parent can be of another type, but
          nothing is inherited from it, it is only used to store the paths once. The parents must
          not form a cycle.
          """
        )
        .asString();
    }
    return new ConfiguredProfile(id, type, parent, p);
  }

  /**
   * Map the user preferences of a profile type. Unset values are the values of {@code base}, the
   * preferences of the parent of the same type, or the ones of the request if there is none. The
   * base is of the type of the profile.
   */
  private static AbstractUserPreferences<?> mapPreferences(
    TransferProfileType type,
    NodeAdapter p,
    AbstractUserPreferences<?> base
  ) {
    return switch (type) {
      case WALK -> mapWalkPreferences(p, (WalkPreferences) base);
      case WHEELCHAIR -> mapWheelchairPreferences(p, (WheelchairPreferences) base);
      case BICYCLE -> mapBicyclePreferences(p, (BicyclePreferences) base);
      case CAR -> mapCarPreferences(p, (CarPreferences) base);
    };
  }

  private static WalkPreferences mapWalkPreferences(NodeAdapter p, WalkPreferences base) {
    var builder = base.copyOf();
    builder.withSpeed(mapSpeed(p, "Walking speed, in meters per second.", base));
    builder.withReluctance(mapReluctance(p, "A multiplier for the cost of walking.", base));
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
    return builder.build();
  }

  private static BicyclePreferences mapBicyclePreferences(NodeAdapter p, BicyclePreferences base) {
    var builder = base.copyOf();
    builder.withSpeed(mapSpeed(p, "Bicycle speed, in meters per second.", base));
    builder.withReluctance(
      mapReluctance(p, "A multiplier for the cost of riding a bicycle.", base)
    );
    builder.withWalk(mapBicycleWalk(p, base.walk()));
    return builder.build();
  }

  private static BicycleWalk mapBicycleWalk(NodeAdapter p, BicycleWalk base) {
    var walk = p
      .of("walk")
      .since(V2_10)
      .summary("The preferences for walking with the bicycle.")
      .description("Used when the traveler pushes the bicycle, for example on stairs.")
      .asObject();
    return BicycleWalk.of(
      Speed.ofMetersPerSecond(
        walk
          .of("speed")
          .since(V2_10)
          .summary("The speed of walking with the bicycle, in meters per second.")
          .asDouble(base.speed().toMetersPerSecond())
      ),
      Reluctance.of(
        walk
          .of("reluctance")
          .since(V2_10)
          .summary("A multiplier for the cost of walking with the bicycle.")
          .description("A higher value avoids walking with the bicycle.")
          .asDouble(base.reluctance().value())
      )
    );
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
}
