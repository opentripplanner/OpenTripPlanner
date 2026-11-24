package org.opentripplanner.transit.transfer.regular.internal;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.opentripplanner.transit.transfer.regular.AlternativePath;
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

  /** Store the alternative paths from {@code fromStop}, see {@link TemplatePathsByStop}. */
  void setAlternativePaths(
    AbstractUserPreferences<?> userPreferences,
    int fromStop,
    Map<Integer, List<AlternativePath<P>>> pathsByToStop
  ) {
    if (pathsByToStop.isEmpty()) {
      return;
    }
    pathsByUserPreferences
      .computeIfAbsent(userPreferences, p -> new TemplatePathsByStop<>())
      .setAlternativePaths(fromStop, pathsByToStop);
  }

  /**
   * The alternative paths of the transfer stored under exactly the given profile, the best path
   * first. Empty if there are none.
   */
  List<AlternativePath<P>> alternativePaths(
    AbstractUserPreferences<?> profile,
    int fromStop,
    int toStop
  ) {
    var paths = pathsByUserPreferences.get(profile);
    return paths == null ? List.of() : paths.alternativePaths(fromStop, toStop);
  }

  /**
   * The stops that have a transfer under exactly the given profile with an alternative path that
   * depends on one of the facility ids.
   */
  Set<Integer> fromStopsDependingOn(AbstractUserPreferences<?> profile, Set<String> facilityIds) {
    var paths = pathsByUserPreferences.get(profile);
    return paths == null ? Set.of() : paths.fromStopsDependingOn(facilityIds);
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
}
