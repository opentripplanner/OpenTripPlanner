package org.opentripplanner.transit.transfer.regular.api;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The configuration of the regular transfers: the profiles to generate transfer paths for, and the
 * max durations of the profile types. Only walk profiles are supported.
 *
 * @param walk                the walk profiles, in the order declared in the configuration. At
 *                            least one is required, and the ids must be unique. The user
 *                            preferences of the profiles must be unique too, but that is checked
 *                            when the profiles are created, since a profile without preferences
 *                            gets them from its request.
 * @param defaultMaxDurations the max durations of the profile types without an override in
 *                            {@code maxDurations}
 * @param maxDurations        the max durations of a profile type, shared by all profiles of the
 *                            type. This is one configuration per type, it replaces the default.
 * @param <R>                 the request type, opaque to the module
 */
public record TransferProfilesParameters<R>(
  List<WalkProfileParameters<R>> walk,
  MaxDurationsPreference defaultMaxDurations,
  Map<TransferProfileType, MaxDurationsPreference> maxDurations
) {
  public TransferProfilesParameters {
    walk = List.copyOf(walk);
    Objects.requireNonNull(defaultMaxDurations);
    maxDurations = maxDurations.isEmpty() ? Map.of() : Map.copyOf(new EnumMap<>(maxDurations));
    if (walk.isEmpty()) {
      throw new IllegalArgumentException("At least one walk transfer profile is required.");
    }
    var ids = new HashSet<String>();
    for (var profile : walk) {
      if (!ids.add(profile.id())) {
        throw new IllegalArgumentException(
          "Duplicate transfer profile id, the ids must be unique: " + profile.id()
        );
      }
    }
  }

  /** The profiles with the default max durations. */
  public TransferProfilesParameters(List<WalkProfileParameters<R>> walk) {
    this(walk, MaxDurationsPreference.DEFAULT, Map.of());
  }

  /** The max durations of {@code type}: its override, or else the default. */
  public MaxDurationsPreference maxDurations(TransferProfileType type) {
    return maxDurations.getOrDefault(type, defaultMaxDurations);
  }
}
