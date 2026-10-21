package org.opentripplanner.transit.transfer.regular.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;

/**
 * Default {@link RegularTransferRepository}: a single long-lived buffer, shared by all
 * transactions and only accessed on the writer thread. It is created once from the build
 * repository, whose paths are the immutable base.
 * <p>
 * Besides the current paths, the buffer tracks the stops whose transfers changed since the last
 * {@link #freeze()}, by profile. The Raptor transfers derived from the paths only need to be
 * updated for these stops.
 *
 * @param <P> the transfer path/template type
 */
class DefaultRegularTransferRepository<P> implements RegularTransferRepository<P> {

  private OverlaidTemplatePaths<P> paths;
  private final Map<AbstractUserPreferences<?>, Set<Integer>> changedFromStops = new HashMap<>();

  DefaultRegularTransferRepository(TemplatePathsByUserProfile<P> base) {
    this.paths = new OverlaidTemplatePaths<>(base);
  }

  @Override
  public void replaceTransfersFrom(
    AbstractUserPreferences<?> profile,
    int fromStop,
    Map<Integer, P> pathsByToStop
  ) {
    paths.replace(profile, fromStop, pathsByToStop);
    markChanged(profile, fromStop);
  }

  @Override
  public void resetTransfersFrom(AbstractUserPreferences<?> profile, int fromStop) {
    if (paths.reset(profile, fromStop)) {
      markChanged(profile, fromStop);
    }
  }

  /** Whether the paths changed since the last {@link #freeze()}. */
  boolean hasChanges() {
    return !changedFromStops.isEmpty();
  }

  /**
   * Discard all changes since the last {@link #freeze()}, and continue from {@code committed}
   * instead - the paths of the last published snapshot. Used to roll back a failed transaction.
   */
  void discardChanges(OverlaidTemplatePaths<P> committed) {
    paths = committed.copy();
    changedFromStops.clear();
  }

  /**
   * Return a copy of the current paths, to be published in an immutable snapshot, and the stops
   * whose transfers changed since the last freeze. Starts tracking the changes anew. Only the
   * lifecycle should have access to this, hence the package local access.
   */
  Frozen<P> freeze() {
    var frozen = new Frozen<>(paths.copy(), Map.copyOf(changedFromStops));
    changedFromStops.clear();
    return frozen;
  }

  private void markChanged(AbstractUserPreferences<?> profile, int fromStop) {
    changedFromStops.computeIfAbsent(profile, p -> new HashSet<>()).add(fromStop);
  }

  /**
   * The paths of a snapshot, and the stops whose transfers changed since the previous snapshot,
   * by profile. The sets of stops must not be modified.
   */
  record Frozen<P>(
    OverlaidTemplatePaths<P> paths,
    Map<AbstractUserPreferences<?>, Set<Integer>> changedFromStops
  ) {}
}
