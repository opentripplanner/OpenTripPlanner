package org.opentripplanner.transit.transfer.regular.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;

/**
 * Default {@link RegularTransferRepository}: the mutable paths of one transaction, only accessed
 * on the writer thread. A repository continues from a snapshot, the base, with a copy of its
 * paths; the first repository continues from the paths generated at graph build instead. Copying
 * the paths copies only the transfers replaced at runtime, see {@link OverlaidTemplatePaths}.
 * <p>
 * Besides the current paths, the repository tracks the stops whose transfers changed since the
 * base, by profile. The Raptor transfers derived from the paths of the base only need to be
 * updated for these stops.
 * <p>
 * A repository is frozen once, and its paths are published in a snapshot without a copy. Hence,
 * a frozen repository cannot be written to again. A repository that is never frozen, because its
 * transaction failed, is discarded with its changes.
 *
 * @param <P> the street path type
 */
class DefaultRegularTransferRepository<P> implements RegularTransferRepository<P> {

  private final OverlaidTemplatePaths<P> paths;

  @Nullable
  private final DefaultRegularTransferRepositorySnapshot<P> base;

  private final Map<AbstractUserPreferences<?>, Set<Integer>> changedFromStops = new HashMap<>();
  private boolean frozen = false;

  /** Create the first repository, with the paths generated at graph build and no base. */
  DefaultRegularTransferRepository(TemplatePathsByUserProfile<P> graphBuildPaths) {
    this(new OverlaidTemplatePaths<>(graphBuildPaths), null);
  }

  private DefaultRegularTransferRepository(
    OverlaidTemplatePaths<P> paths,
    @Nullable DefaultRegularTransferRepositorySnapshot<P> base
  ) {
    this.paths = paths;
    this.base = base;
  }

  /** Create a repository continuing from {@code base}, with a copy of its paths. */
  static <P> DefaultRegularTransferRepository<P> continueFrom(
    DefaultRegularTransferRepositorySnapshot<P> base
  ) {
    return new DefaultRegularTransferRepository<>(base.paths().copy(), base);
  }

  @Override
  public void replaceTransfersFrom(
    AbstractUserPreferences<?> profile,
    int fromStop,
    Map<Integer, P> pathsByToStop
  ) {
    assertNotFrozen();
    paths.replace(profile, fromStop, pathsByToStop);
    markChanged(profile, fromStop);
  }

  @Override
  public void resetTransfersFrom(AbstractUserPreferences<?> profile, int fromStop) {
    assertNotFrozen();
    if (paths.reset(profile, fromStop)) {
      markChanged(profile, fromStop);
    }
  }

  /**
   * Return the paths, to be published in an immutable snapshot, the base, and the stops whose
   * transfers changed since the base. The paths are not copied, so the repository cannot be
   * written to or frozen afterward. Only the lifecycle should have access to this, hence the
   * package local access.
   *
   * @throws IllegalStateException if the repository is already frozen
   */
  Frozen<P> freeze() {
    assertNotFrozen();
    frozen = true;
    return new Frozen<>(paths, base, Map.copyOf(changedFromStops));
  }

  private void markChanged(AbstractUserPreferences<?> profile, int fromStop) {
    changedFromStops.computeIfAbsent(profile, p -> new HashSet<>()).add(fromStop);
  }

  private void assertNotFrozen() {
    if (frozen) {
      throw new IllegalStateException(
        "The repository is frozen, write to the repository of a new transaction instead."
      );
    }
  }

  /**
   * The paths of a snapshot, the snapshot they continue from - {@code null} for the paths
   * generated at graph build - and the stops whose transfers changed since then, by profile. The
   * sets of stops must not be modified.
   */
  record Frozen<P>(
    OverlaidTemplatePaths<P> paths,
    @Nullable DefaultRegularTransferRepositorySnapshot<P> base,
    Map<AbstractUserPreferences<?>, Set<Integer>> changedFromStops
  ) {}
}
