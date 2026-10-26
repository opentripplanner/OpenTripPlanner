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
public final class TransferProfilesParameters<R> {

  private final List<TransferProfileParameters<R>> profiles;
  private final PlainMaxDuration defaultMaxDurations;
  private final Map<TransferProfileType, MaxDurationsPreference> maxDurations;
  private final DeduplicationLimit deduplicationLimit;
  private final List<WarmupParameters> warmup;

  /**
   * @param profiles            the profiles of all types, in the order declared in the
   *                            configuration. At least one is required, and the ids must be
   *                            unique. The user preferences of the profiles must be unique too,
   *                            but that is checked when the profiles are created, since a profile
   *                            without preferences gets them from its request.
   * @param defaultMaxDurations the max duration of the profile types without an override in
   *                            {@code maxDurations}. It is the same between all stops.
   * @param maxDurations        the max durations of a profile type, shared by all profiles of the
   *                            type. This is one configuration per type, it replaces the default,
   *                            and must be of the kind of the type: {@link BicycleMaxDurations}
   *                            for bicycle and {@link CarMaxDuration} for car, and
   *                            {@link PlainMaxDuration} for the other types.
   * @param deduplicationLimit  when the path of a parent profile is used instead of a path of the
   *                            profile itself, see {@link DeduplicationLimit}
   * @param warmup              the preferences to create Raptor transfers for when the server
   *                            starts, see {@link WarmupParameters}. The ids must be unique among
   *                            the profiles and the warmup entries, and the parent of an entry
   *                            must be a profile.
   */
  public TransferProfilesParameters(
    List<TransferProfileParameters<R>> profiles,
    PlainMaxDuration defaultMaxDurations,
    Map<TransferProfileType, MaxDurationsPreference> maxDurations,
    DeduplicationLimit deduplicationLimit,
    List<WarmupParameters> warmup
  ) {
    this.profiles = List.copyOf(profiles);
    this.warmup = List.copyOf(warmup);
    this.defaultMaxDurations = Objects.requireNonNull(defaultMaxDurations);
    this.deduplicationLimit = Objects.requireNonNull(deduplicationLimit);
    this.maxDurations = maxDurations.isEmpty() ? Map.of() : Map.copyOf(new EnumMap<>(maxDurations));
    this.maxDurations.forEach(TransferProfilesParameters::validateMaxDurationsOfType);
    validateProfiles(this.profiles, this.warmup);
  }

  /** The profiles with the default max durations and deduplication limit. */
  public TransferProfilesParameters(List<TransferProfileParameters<R>> profiles) {
    this(profiles, MaxDurationsPreference.DEFAULT, Map.of());
  }

  /** The profiles with the default deduplication limit. */
  public TransferProfilesParameters(
    List<TransferProfileParameters<R>> profiles,
    PlainMaxDuration defaultMaxDurations,
    Map<TransferProfileType, MaxDurationsPreference> maxDurations
  ) {
    this(profiles, defaultMaxDurations, maxDurations, DeduplicationLimit.DEFAULT, List.of());
  }

  /** The profiles without warmup entries. */
  public TransferProfilesParameters(
    List<TransferProfileParameters<R>> profiles,
    PlainMaxDuration defaultMaxDurations,
    Map<TransferProfileType, MaxDurationsPreference> maxDurations,
    DeduplicationLimit deduplicationLimit
  ) {
    this(profiles, defaultMaxDurations, maxDurations, deduplicationLimit, List.of());
  }

  public List<TransferProfileParameters<R>> profiles() {
    return profiles;
  }

  /** The max duration of the profile types without an override, see {@link #maxDurations}. */
  public PlainMaxDuration defaultMaxDurations() {
    return defaultMaxDurations;
  }

  public DeduplicationLimit deduplicationLimit() {
    return deduplicationLimit;
  }

  public List<WarmupParameters> warmup() {
    return warmup;
  }

  /**
   * The max durations of {@code type}: its override, or else the default duration, used in the way
   * of the type.
   */
  public MaxDurationsPreference maxDurations(TransferProfileType type) {
    var override = maxDurations.get(type);
    return override != null
      ? override
      : MaxDurationsPreference.of(type, defaultMaxDurations.maxDuration());
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
    List<TransferProfileParameters<R>> profiles,
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
    ParentOrder.parentsFirst(
      profiles,
      TransferProfileParameters::id,
      TransferProfileParameters::parent
    );
    for (var entry : warmup) {
      if (!ids.add(entry.id())) {
        throw new IllegalArgumentException(
          "Duplicate transfer profile id, the ids must be unique: " + entry.id()
        );
      }
    }
    var profileIds = profiles
      .stream()
      .map(TransferProfileParameters::id)
      .collect(Collectors.toSet());
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
    var that = (TransferProfilesParameters<?>) o;
    return (
      profiles.equals(that.profiles) &&
      defaultMaxDurations.equals(that.defaultMaxDurations) &&
      maxDurations.equals(that.maxDurations) &&
      deduplicationLimit.equals(that.deduplicationLimit) &&
      warmup.equals(that.warmup)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(profiles, defaultMaxDurations, maxDurations, deduplicationLimit, warmup);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TransferProfilesParameters.class)
      .addCol("profiles", profiles)
      .addObj("defaultMaxDurations", defaultMaxDurations)
      .addObj("maxDurations", maxDurations)
      .addObj("deduplicationLimit", deduplicationLimit)
      .addCol("warmup", warmup)
      .toString();
  }
}
