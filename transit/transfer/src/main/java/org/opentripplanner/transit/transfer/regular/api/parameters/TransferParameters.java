package org.opentripplanner.transit.transfer.regular.api.parameters;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.core.model.basic.CostLinearFunction;
import org.opentripplanner.transit.transfer.regular.api.maxduration.BicycleMaxDurations;
import org.opentripplanner.transit.transfer.regular.api.maxduration.CarMaxDuration;
import org.opentripplanner.transit.transfer.regular.api.maxduration.MaxDurationsPreference;
import org.opentripplanner.transit.transfer.regular.api.maxduration.PlainMaxDuration;
import org.opentripplanner.utils.collection.ParentOrder;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The configuration of the regular transfers: the profiles to generate transfer paths for, and the
 * max durations of the profile types.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 *
 * @param <R> the request type, opaque to the module
 */
public final class TransferParameters<R> {

  /**
   * A path of the parent within 15 seconds, and 2% of its cost, of the best path of a profile is
   * equivalent: the function is the tolerance, in generalized cost, of a path of the parent that
   * costs {@code x}, see {@link TransferProfile#reuseLimit()}. This is the limit of a profile that
   * has none of its own and does not inherit one.
   */
  public static final CostLinearFunction DEFAULT_REUSE_LIMIT = CostLinearFunction.of(
    Duration.ofSeconds(15),
    0.02
  );

  /**
   * The profile types that use the closest profile of another type, if they have no profile, in the
   * default configuration of a single walk profile: a request for a wheelchair or a bicycle uses the
   * paths of the walk profile, and creates its own entries in the cache.
   */
  public static final Set<TransferProfileType> DEFAULT_FALLBACK_TYPES = Collections.unmodifiableSet(
    EnumSet.of(TransferProfileType.WHEELCHAIR, TransferProfileType.BICYCLE)
  );

  /**
   * The max duration of the transfers of a profile type, if it is not set: how far from a stop the
   * transfers are searched.
   */
  public static final Duration DEFAULT_MAX_DURATION = Duration.ofMinutes(30);

  private final List<TransferProfile<R>> profiles;
  private final Map<TransferProfileType, MaxDurationsPreference> maxDurations;
  private final List<WarmupParameters> warmup;
  private final Set<TransferProfileType> fallbackToClosestType;

  /**
   * @param profiles           the profiles of all types, in the order declared in the
   *                           configuration. At least one is required, and the ids must be unique.
   *                           The user preferences of the profiles must be unique too, but that is
   *                           checked when the profiles are created, since a profile without
   *                           preferences gets them from its request.
   * @param maxDurations       the max durations of a profile type, shared by all profiles of the
   *                           type. Each type with a profile needs one, and it must be of the kind
   *                           of the type: {@link BicycleMaxDurations} for bicycle and
   *                           {@link CarMaxDuration} for car, and {@link PlainMaxDuration} for the
   *                           other types.
   * @param fallbackToClosestType the profile types that use the stored paths of the closest profile
   *                           of another type if they have no profile. A request for any other
   *                           type without a profile fails, there are no transfers for the type.
   * @param warmup             the preferences to create Raptor transfers for when the server
   *                           starts, see {@link WarmupParameters}. The ids must be unique among
   *                           the profiles and the warmup entries, and the parent of an entry must
   *                           be a profile.
   */
  public TransferParameters(
    List<TransferProfile<R>> profiles,
    Map<TransferProfileType, MaxDurationsPreference> maxDurations,
    List<WarmupParameters> warmup,
    Set<TransferProfileType> fallbackToClosestType
  ) {
    this.profiles = List.copyOf(profiles);
    this.warmup = List.copyOf(warmup);
    this.fallbackToClosestType = Set.copyOf(fallbackToClosestType);
    this.maxDurations = maxDurations.isEmpty() ? Map.of() : Map.copyOf(new EnumMap<>(maxDurations));
    this.maxDurations.forEach(TransferParameters::validateMaxDurationsOfType);
    validateProfiles(this.profiles, this.warmup);
    validateAllTypesHaveMaxDurations(this.profiles, this.maxDurations);
  }

  /**
   * The profiles with the 30 minute max duration of each type, see
   * {@link MaxDurationsPreference#defaultFor}. This is the implicit configuration of a single walk
   * profile.
   */
  public TransferParameters(List<TransferProfile<R>> profiles) {
    this(profiles, defaultMaxDurations(profiles));
  }

  /** The profiles without warmup entries. */
  public TransferParameters(
    List<TransferProfile<R>> profiles,
    Map<TransferProfileType, MaxDurationsPreference> maxDurations
  ) {
    this(profiles, maxDurations, List.of());
  }

  /** The profiles and the warmup entries, a request for a type without profiles fails. */
  public TransferParameters(
    List<TransferProfile<R>> profiles,
    Map<TransferProfileType, MaxDurationsPreference> maxDurations,
    List<WarmupParameters> warmup
  ) {
    this(profiles, maxDurations, warmup, Set.of());
  }

  /**
   * The default configuration of a single walk profile, used if no profile type is configured. A
   * request for a wheelchair or a bicycle falls back to the paths of the walk profile, see
   * {@link #DEFAULT_FALLBACK_TYPES}.
   */
  public static <R> TransferParameters<R> defaultConfiguration(TransferProfile<R> walk) {
    var profiles = List.of(walk);
    return new TransferParameters<>(
      profiles,
      defaultMaxDurations(profiles),
      List.of(),
      DEFAULT_FALLBACK_TYPES
    );
  }

  public List<TransferProfile<R>> profiles() {
    return profiles;
  }

  public List<WarmupParameters> warmup() {
    return warmup;
  }

  /**
   * The profile types that use the stored paths of the closest profile of another type, see
   * {@link TransferProfileType#normalizedDistance}, if they have no profile. A request for any
   * other type without a profile fails. Empty if a profile type is configured: each type used by
   * the requests must be configured then.
   */
  public Set<TransferProfileType> fallbackToClosestType() {
    return fallbackToClosestType;
  }

  /**
   * The max durations of {@code type}.
   *
   * @throws IllegalArgumentException if there is no profile of the type, and so no max durations
   */
  public MaxDurationsPreference maxDurations(TransferProfileType type) {
    var result = maxDurations.get(type);
    if (result == null) {
      throw new IllegalArgumentException("There are no max durations for the type " + type);
    }
    return result;
  }

  private static <R> Map<TransferProfileType, MaxDurationsPreference> defaultMaxDurations(
    List<TransferProfile<R>> profiles
  ) {
    var result = new EnumMap<TransferProfileType, MaxDurationsPreference>(
      TransferProfileType.class
    );
    for (var profile : profiles) {
      result.computeIfAbsent(profile.type(), MaxDurationsPreference::defaultFor);
    }
    return result;
  }

  private static <R> void validateAllTypesHaveMaxDurations(
    List<TransferProfile<R>> profiles,
    Map<TransferProfileType, MaxDurationsPreference> maxDurations
  ) {
    for (var profile : profiles) {
      if (!maxDurations.containsKey(profile.type())) {
        throw new IllegalArgumentException(
          "The profile type '" +
            profile.type().name().toLowerCase() +
            "' has profiles, but no max duration is set for it, see '" +
            profile.id() +
            "'."
        );
      }
    }
  }

  private static void validateMaxDurationsOfType(
    TransferProfileType type,
    MaxDurationsPreference maxDurations
  ) {
    boolean valid = switch (type) {
      case BICYCLE -> maxDurations instanceof BicycleMaxDurations;
      case CAR -> maxDurations instanceof CarMaxDuration;
      case WALK, WHEELCHAIR -> maxDurations instanceof PlainMaxDuration;
    };
    if (!valid) {
      throw new IllegalArgumentException(
        "The max durations of the type " +
          type.name().toLowerCase() +
          " are of the wrong kind: " +
          maxDurations
      );
    }
  }

  private static <R> void validateProfiles(
    List<TransferProfile<R>> profiles,
    List<WarmupParameters> warmup
  ) {
    if (profiles.isEmpty()) {
      throw new IllegalArgumentException("At least one transfer profile is required.");
    }
    var ids = new HashSet<String>();
    for (var profile : profiles) {
      if (!ids.add(profile.id())) {
        throw new IllegalArgumentException(
          "Duplicate transfer profile id, the ids must be unique: " + profile.id()
        );
      }
    }
    // Check that the parents exist, and that they do not form a cycle
    ParentOrder.parentsFirst(profiles, TransferProfile::id, TransferProfile::parent);
    for (var entry : warmup) {
      if (!ids.add(entry.id())) {
        throw new IllegalArgumentException(
          "Duplicate transfer profile id, the ids must be unique: " + entry.id()
        );
      }
    }
    var profileIds = profiles.stream().map(TransferProfile::id).collect(Collectors.toSet());
    for (var entry : warmup) {
      if (!profileIds.contains(entry.parent())) {
        throw new IllegalArgumentException(
          "The parent '" + entry.parent() + "' of the warmup '" + entry.id() + "' is not a profile"
        );
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var that = (TransferParameters<?>) o;
    return (
      profiles.equals(that.profiles) &&
      maxDurations.equals(that.maxDurations) &&
      warmup.equals(that.warmup) &&
      fallbackToClosestType.equals(that.fallbackToClosestType)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(profiles, maxDurations, warmup, fallbackToClosestType);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TransferParameters.class)
      .addCol("profiles", profiles)
      .addObj("maxDurations", maxDurations)
      .addCol("warmup", warmup)
      .addCol("fallbackToClosestType", fallbackToClosestType)
      .toString();
  }
}
