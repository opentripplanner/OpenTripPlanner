package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.transit.transfer.regular.AlternativePath;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;

/**
 * The path templates of a snapshot with its closed facilities applied: the one path each transfer
 * uses while the facilities are closed.
 * <p>
 * The profile decides the path, never the request. The alternative paths of a transfer are stored
 * in the cost order of the profile, the best path first, see
 * {@link RegularTransferBuildRepository#setAlternativePaths}.
 * The open path is the first of them that depends on no closed facility; the pair has no transfer
 * if every path depends on one. A transfer without alternative paths, including every transfer
 * from a stop whose transfers were replaced at runtime, uses its stored path whatever is closed.
 * The request preferences only re-cost the chosen path.
 * <p>
 * The instance is immutable as long as the given paths are not written to, and then thread-safe.
 *
 * @param <P> the street path type
 */
final class OpenTemplatePaths<P> {

  private final OverlaidTemplatePaths<P> paths;
  private final Set<String> closedFacilityIds;

  /** The closed facility ids are copied. */
  OpenTemplatePaths(OverlaidTemplatePaths<P> paths, Set<String> closedFacilityIds) {
    this.paths = paths;
    this.closedFacilityIds = Set.copyOf(closedFacilityIds);
  }

  /** The paths with every facility open. */
  OpenTemplatePaths(OverlaidTemplatePaths<P> paths) {
    this(paths, Set.of());
  }

  /**
   * The path the transfer from {@code fromStop} to {@code toStop} uses, given the stored path of
   * the pair. {@code null} if every path of the transfer depends on a closed facility.
   */
  @Nullable
  private P openPath(AbstractUserPreferences<?> profile, int fromStop, int toStop, P storedPath) {
    if (closedFacilityIds.isEmpty()) {
      return storedPath;
    }
    var alternatives = paths.alternativePaths(profile, fromStop, toStop);
    if (alternatives.isEmpty()) {
      return storedPath;
    }
    return alternatives
      .stream()
      .filter(it -> Collections.disjoint(it.closableFacilityIds(), closedFacilityIds))
      .map(AlternativePath::path)
      .findFirst()
      .orElse(null);
  }

  /**
   * The open path from {@code fromStop} to {@code toStop} under {@code profile}. {@code null} if
   * there is no stored path, or if every path of the transfer depends on a closed facility.
   */
  @Nullable
  P findPath(@Nullable AbstractUserPreferences<?> profile, int fromStop, int toStop) {
    var stored = paths.findPath(profile, fromStop, toStop);
    return stored == null ? null : openPath(profile, fromStop, toStop, stored);
  }

  /**
   * The open paths from {@code fromStop} under {@code profile}, by {@code toStop}. The pairs whose
   * paths all depend on a closed facility are left out. The returned map must not be modified.
   */
  Map<Integer, P> pathsFrom(AbstractUserPreferences<?> profile, int fromStop) {
    var stored = paths.pathsFrom(profile, fromStop);
    if (closedFacilityIds.isEmpty()) {
      return stored;
    }
    var result = new HashMap<Integer, P>();
    stored.forEach((toStop, path) -> {
      var open = openPath(profile, fromStop, toStop, path);
      if (open != null) {
        result.put(toStop, open);
      }
    });
    return result;
  }

  /** Call {@code consumer} for the open path of every transfer under {@code profile}. */
  void forEachPath(
    AbstractUserPreferences<?> profile,
    OverlaidTemplatePaths.PathConsumer<P> consumer
  ) {
    paths.forEachPath(profile, (fromStop, toStop, path) -> {
      var open = openPath(profile, fromStop, toStop, path);
      if (open != null) {
        consumer.accept(fromStop, toStop, open);
      }
    });
  }
}
