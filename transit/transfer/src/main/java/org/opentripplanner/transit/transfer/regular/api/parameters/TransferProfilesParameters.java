package org.opentripplanner.transit.transfer.regular.api.parameters;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opentripplanner.utils.collection.ParentOrder;

/**
 * The configuration of the regular transfers: the profiles to generate transfer paths for, and the
 * max durations of the profile types.
 *
 * @param profiles            the profiles of all types, in the order declared in the
 *                            configuration. At least one is required, and the ids must be unique.
 *                            The user preferences of the profiles must be unique too, but that is
 *                            checked when the profiles are created, since a profile without
 *                            preferences gets them from its request.
 * @param defaultMaxDurations the max durations of the profile types without an override in
 *                            {@code maxDurations}
 * @param maxDurations        the max durations of a profile type, shared by all profiles of the
 *                            type. This is one configuration per type, it replaces the default.
 * @param deduplicationLimit  when the path of a parent profile is used instead of a path of the
 *                            profile itself, see {@link DeduplicationLimit}
 * @param warmup              the preferences to create Raptor transfers for when the server starts,
 *                            see {@link WarmupParameters}. The ids must be unique among the
 *                            profiles and the warmup entries, and the parent of an entry must be a
 *                            profile.
 * @param <R>                 the request type, opaque to the module
 */
public record TransferProfilesParameters<R>(
  List<TransferProfileParameters<R>> profiles,
  MaxDurationsPreference defaultMaxDurations,
  Map<TransferProfileType, MaxDurationsPreference> maxDurations,
  DeduplicationLimit deduplicationLimit,
  List<WarmupParameters> warmup
) {
  public TransferProfilesParameters {
    profiles = List.copyOf(profiles);
    warmup = List.copyOf(warmup);
    Objects.requireNonNull(defaultMaxDurations);
    Objects.requireNonNull(deduplicationLimit);
    maxDurations = maxDurations.isEmpty() ? Map.of() : Map.copyOf(new EnumMap<>(maxDurations));
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

  /** The profiles with the default max durations and deduplication limit. */
  public TransferProfilesParameters(List<TransferProfileParameters<R>> profiles) {
    this(profiles, MaxDurationsPreference.DEFAULT, Map.of());
  }

  /** The profiles with the default deduplication limit. */
  public TransferProfilesParameters(
    List<TransferProfileParameters<R>> profiles,
    MaxDurationsPreference defaultMaxDurations,
    Map<TransferProfileType, MaxDurationsPreference> maxDurations
  ) {
    this(profiles, defaultMaxDurations, maxDurations, DeduplicationLimit.DEFAULT, List.of());
  }

  /** The profiles without warmup entries. */
  public TransferProfilesParameters(
    List<TransferProfileParameters<R>> profiles,
    MaxDurationsPreference defaultMaxDurations,
    Map<TransferProfileType, MaxDurationsPreference> maxDurations,
    DeduplicationLimit deduplicationLimit
  ) {
    this(profiles, defaultMaxDurations, maxDurations, deduplicationLimit, List.of());
  }

  /** The max durations of {@code type}: its override, or else the default. */
  public MaxDurationsPreference maxDurations(TransferProfileType type) {
    return maxDurations.getOrDefault(type, defaultMaxDurations);
  }
}
