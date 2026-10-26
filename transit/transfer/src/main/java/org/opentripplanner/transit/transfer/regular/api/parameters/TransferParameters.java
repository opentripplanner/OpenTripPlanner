package org.opentripplanner.transit.transfer.regular.api.parameters;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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

  private final List<TransferProfile<R>> profiles;
  private final Map<TransferProfileType, MaxDurationsPreference> maxDurations;
  private final DeduplicationLimit deduplicationLimit;
  private final List<WarmupParameters> warmup;

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
   * @param deduplicationLimit when the path of a parent profile is used instead of a path of the
   *                           profile itself, see {@link DeduplicationLimit}
   * @param warmup             the preferences to create Raptor transfers for when the server
   *                           starts, see {@link WarmupParameters}. The ids must be unique among
   *                           the profiles and the warmup entries, and the parent of an entry must
   *                           be a profile.
   */
  public TransferParameters(
    List<TransferProfile<R>> profiles,
    Map<TransferProfileType, MaxDurationsPreference> maxDurations,
    DeduplicationLimit deduplicationLimit,
    List<WarmupParameters> warmup
  ) {
    this.profiles = List.copyOf(profiles);
    this.warmup = List.copyOf(warmup);
    this.deduplicationLimit = Objects.requireNonNull(deduplicationLimit);
    this.maxDurations = maxDurations.isEmpty() ? Map.of() : Map.copyOf(new EnumMap<>(maxDurations));
    this.maxDurations.forEach(TransferParameters::validateMaxDurationsOfType);
    validateProfiles(this.profiles, this.warmup);
    validateAllTypesHaveMaxDurations(this.profiles, this.maxDurations);
  }

  /**
   * The profiles with the 30 minute max duration of each type, see
   * {@link MaxDurationsPreference#defaultFor}, and the default deduplication limit. This is the
   * implicit configuration of a single walk profile.
   */
  public TransferParameters(List<TransferProfile<R>> profiles) {
    this(profiles, defaultMaxDurations(profiles));
  }

  /** The profiles with the default deduplication limit. */
  public TransferParameters(
    List<TransferProfile<R>> profiles,
    Map<TransferProfileType, MaxDurationsPreference> maxDurations
  ) {
    this(profiles, maxDurations, DeduplicationLimit.DEFAULT);
  }

  /** The profiles without warmup entries. */
  public TransferParameters(
    List<TransferProfile<R>> profiles,
    Map<TransferProfileType, MaxDurationsPreference> maxDurations,
    DeduplicationLimit deduplicationLimit
  ) {
    this(profiles, maxDurations, deduplicationLimit, List.of());
  }

  public List<TransferProfile<R>> profiles() {
    return profiles;
  }

  public DeduplicationLimit deduplicationLimit() {
    return deduplicationLimit;
  }

  public List<WarmupParameters> warmup() {
    return warmup;
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
      deduplicationLimit.equals(that.deduplicationLimit) &&
      warmup.equals(that.warmup)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(profiles, maxDurations, deduplicationLimit, warmup);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TransferParameters.class)
      .addCol("profiles", profiles)
      .addObj("maxDurations", maxDurations)
      .addObj("deduplicationLimit", deduplicationLimit)
      .addCol("warmup", warmup)
      .toString();
  }
}
