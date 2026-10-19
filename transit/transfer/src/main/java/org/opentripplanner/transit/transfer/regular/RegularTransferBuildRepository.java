package org.opentripplanner.transit.transfer.regular;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot.StoredPath;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;

/**
 * The regular-transfer path templates generated at graph build, keyed by
 * {@code (userPreferences, fromStop, toStop)}, and serialized with the graph. Written by the
 * {@link TransferGenerator}.
 * <p>
 * This repository is only used at graph build, it is not the runtime repository. When the
 * application starts, it is converted into the initial snapshot of the
 * {@link RegularTransferRepository}, which is then only updated inside the transaction framework -
 * see {@link #createInitialSnapshot}. The conversion seals this repository: writing to it afterward
 * throws an {@link IllegalStateException}.
 *
 * @param <P> the transfer path/template type
 */
public interface RegularTransferBuildRepository<P> extends Serializable {
  /**
   * Create the initial snapshot of the {@link RegularTransferRepository} from the paths generated
   * at graph build, and seal the build repository: from now on regular transfers can only be
   * updated through the runtime repository. Writes to the build repository after this throw - and
   * even without the seal they could not change the snapshot, which owns a copy of the paths.
   */
  RegularTransferRepositorySnapshot<P> createInitialSnapshot();

  /**
   * Store the path templates for all transfers from {@code fromStop}, keyed by {@code toStop}.
   * Existing paths to other stops are kept. The given map is copied, not retained.
   *
   * @throws IllegalStateException if the repository is sealed
   */
  void setPaths(
    AbstractUserPreferences<?> userPreferences,
    int fromStop,
    Map<Integer, P> pathsByToStop
  );

  /**
   * Whether {@code fromStop} has at least one outgoing transfer, under any user preferences. Used at graph
   * build to flag stops that could not be linked to any other stop.
   */
  boolean hasTransfersFrom(int fromStop);

  /**
   * Every stored {@code (fromStop, toStop, path)} under the user preferences closest to
   * {@code userPreferences}. Empty if no paths are stored.
   */
  List<StoredPath<P>> pathsFor(AbstractUserPreferences<?> userPreferences);

  /** The number of stored paths, under all user preferences. */
  int calculateNumberOfTransferPaths();

  /** The number of paths stored under exactly the given user preferences. */
  int calculateNumberOfTransferPaths(AbstractUserPreferences<?> userPreferences);
}
