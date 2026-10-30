package org.opentripplanner.transit.transfer.regular.internal;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;

/**
 * The {@code P} path templates generated at graph build, keyed by
 * {@code (userPreferences, fromStop, toStop)}. Each user preferences key is a transfer profile,
 * its paths are held by a {@link TemplatePathsByStop}.
 * <p>
 * Written only at graph build, by the build repository. Once the build repository is sealed, this
 * map is never modified again: it is the immutable base the runtime repository and all its
 * snapshots share, see {@link OverlaidTemplatePaths}. The map can be large (all stops x nearby
 * stops), so it is never copied.
 * <p>
 * This class is not thread-safe while it is written to.
 *
 * @param <P> the street path type
 */
final class TemplatePathsByUserProfile<P> implements Serializable {

  private final Map<AbstractUserPreferences<?>, TemplatePathsByStop<P>> pathsByUserPreferences =
    new HashMap<>();

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
    pathsByUserPreferences
      .computeIfAbsent(userPreferences, p -> new TemplatePathsByStop<>())
      .setPaths(fromStop, pathsByToStop);
  }

  /** The user preferences paths are stored under, the profiles. */
  Set<AbstractUserPreferences<?>> profiles() {
    return Collections.unmodifiableSet(pathsByUserPreferences.keySet());
  }

  /**
   * The paths from {@code fromStop} stored under exactly the given profile, by {@code toStop}.
   * Empty if the profile or the stop is unknown. The returned map must not be modified.
   */
  Map<Integer, P> pathsFrom(AbstractUserPreferences<?> profile, int fromStop) {
    var paths = pathsByUserPreferences.get(profile);
    return paths == null ? Map.of() : paths.pathsFrom(fromStop);
  }

  /**
   * Call {@code consumer} with the paths of every {@code fromStop} stored under exactly the given
   * profile, by {@code toStop}. The maps must not be modified.
   */
  void forEachFromStop(
    AbstractUserPreferences<?> profile,
    BiConsumer<Integer, Map<Integer, P>> consumer
  ) {
    var paths = pathsByUserPreferences.get(profile);
    if (paths != null) {
      paths.forEachFromStop(consumer);
    }
  }

  /** The number of paths stored under exactly the given user preferences. */
  int size(AbstractUserPreferences<?> userPreferences) {
    var paths = pathsByUserPreferences.get(userPreferences);
    return paths == null ? 0 : paths.size();
  }

  /**
   * The profile closest to the given user preferences: the preferences themselves if paths are
   * stored under them, otherwise the profile of the same type with the smallest
   * {@link AbstractUserPreferences#normalizedDistance}. {@code null} if no paths are stored.
   *
   * @throws IllegalArgumentException if paths are stored, but not for the type of the preferences.
   *                                  A request can only use the transfers of a configured type.
   */
  @Nullable
  AbstractUserPreferences<?> closestProfile(AbstractUserPreferences<?> userPreferences) {
    if (pathsByUserPreferences.containsKey(userPreferences)) {
      return userPreferences;
    }
    if (pathsByUserPreferences.isEmpty()) {
      return null;
    }
    return pathsByUserPreferences
      .keySet()
      .stream()
      .filter(profile -> profile.type() == userPreferences.type())
      .min(Comparator.comparingDouble(userPreferences::normalizedDistance))
      .orElseThrow(() ->
        new IllegalArgumentException(
          "There is no transfer profile of the type " +
            userPreferences.type() +
            ", the transfers of that type are not configured."
        )
      );
  }
}
