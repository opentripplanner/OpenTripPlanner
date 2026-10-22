package org.opentripplanner.transit.transfer.regular.internal;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;

/**
 * The {@code P} path templates generated at graph build, keyed by
 * {@code (userPreferences, fromStop, toStop)}. Each user preferences key is a transfer profile.
 * <p>
 * Written only at graph build, by the build repository. Once the build repository is sealed, this
 * map is never modified again: it is the immutable base the runtime repository and all its
 * snapshots share, see {@link OverlaidTemplatePaths}. The map can be large (all stops x nearby
 * stops), so it is never copied.
 * <p>
 * This class is not thread-safe while it is written to.
 *
 * @param <P> the transfer path/template type
 */
final class TemplatePathsByUserProfile<P> implements Serializable {

  private final Map<
    AbstractUserPreferences<?>,
    Map<Integer, Map<Integer, P>>
  > pathsByUserPreferences;

  TemplatePathsByUserProfile() {
    this(new HashMap<>());
  }

  private TemplatePathsByUserProfile(
    Map<AbstractUserPreferences<?>, Map<Integer, Map<Integer, P>>> pathsByUserPreferences
  ) {
    this.pathsByUserPreferences = pathsByUserPreferences;
  }

  /**
   * Store the paths from {@code fromStop}, keyed by {@code toStop}; paths to other stops are kept.
   * The given map is copied, not retained.
   */
  void setPaths(
    AbstractUserPreferences<?> userPreferences,
    int fromStop,
    Map<Integer, P> pathsByToStop
  ) {
    if (pathsByToStop.isEmpty()) {
      return;
    }
    var byFromStop = pathsByUserPreferences.computeIfAbsent(userPreferences, p -> new HashMap<>());
    // Never modify a stored toStop map, it may be shared with another map - replace it.
    var byToStop = new HashMap<>(byFromStop.getOrDefault(fromStop, Map.of()));
    byToStop.putAll(pathsByToStop);
    byFromStop.put(fromStop, byToStop);
  }

  /** The user preferences paths are stored under, the profiles. */
  Set<AbstractUserPreferences<?>> profiles() {
    return Collections.unmodifiableSet(pathsByUserPreferences.keySet());
  }

  /**
   * The paths stored under exactly the given profile, by {@code fromStop} and {@code toStop}.
   * Empty if the profile is unknown. The returned map must not be modified.
   */
  Map<Integer, Map<Integer, P>> pathsByFromStop(AbstractUserPreferences<?> profile) {
    return pathsByUserPreferences.getOrDefault(profile, Map.of());
  }

  boolean hasTransfersFrom(int fromStop) {
    return pathsByUserPreferences
      .values()
      .stream()
      .anyMatch(byFromStop -> !byFromStop.getOrDefault(fromStop, Map.of()).isEmpty());
  }

  /** The number of paths stored under exactly the given user preferences. */
  int size(AbstractUserPreferences<?> userPreferences) {
    return pathsByUserPreferences
      .getOrDefault(userPreferences, Map.of())
      .values()
      .stream()
      .mapToInt(Map::size)
      .sum();
  }

  /** The number of stored paths, under all user preferences. */
  int size() {
    return pathsByUserPreferences
      .values()
      .stream()
      .flatMap(byFromStop -> byFromStop.values().stream())
      .mapToInt(Map::size)
      .sum();
  }

  /**
   * The profile closest to the given user preferences: the preferences themselves if paths are
   * stored under them, otherwise the profile with the smallest
   * {@link AbstractUserPreferences#normalizedDistance}. {@code null} if no paths are stored.
   */
  @Nullable
  AbstractUserPreferences<?> closestProfile(AbstractUserPreferences<?> userPreferences) {
    if (pathsByUserPreferences.containsKey(userPreferences)) {
      return userPreferences;
    }
    return pathsByUserPreferences
      .keySet()
      .stream()
      .min(Comparator.comparingDouble(userPreferences::normalizedDistance))
      .orElse(null);
  }
}
