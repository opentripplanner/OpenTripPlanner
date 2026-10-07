package org.opentripplanner.transit.transfer.regular;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot.StoredPath;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

/**
 * The regular-transfer path templates generated at graph build, keyed by
 * {@code (profileType, fromStop, toStop)}, and serialized with the graph. Written by the
 * {@link TransferGenerator}.
 * <p>
 * This repository is only used at graph build, it is not the runtime repository. When the
 * application starts, it is converted into the initial snapshot of the
 * {@link RegularTransferRepository}, which is then only updated inside the transaction framework -
 * see {@link #createInitialSnapshot}.
 *
 * @param <P> the transfer path/template type
 */
public interface RegularTransferBuildRepository<P> extends Serializable {
  /**
   * Create the initial snapshot of the {@link RegularTransferRepository} from the paths generated
   * at graph build. Writes to the build repository after this can not change the snapshot, which
   * owns a copy of the paths.
   */
  RegularTransferRepositorySnapshot<P> createInitialSnapshot();

  /**
   * Store the path templates for all transfers from {@code fromStop}, keyed by {@code toStop}.
   * Existing paths to other stops are kept. The given map is copied, not retained.
   */
  void setPaths(TransferProfileType profileType, int fromStop, Map<Integer, P> pathsByToStop);

  /**
   * Whether {@code fromStop} has at least one outgoing transfer, in any profile. Used at graph
   * build to flag stops that could not be linked to any other stop.
   */
  boolean hasTransfersFrom(int fromStop);

  /** Every stored {@code (fromStop, toStop, path)} for a profile. Empty if it has none. */
  List<StoredPath<P>> pathsFor(TransferProfileType profileType);

  /** The number of stored paths, in all profiles. */
  int calculateNumberOfTransferPaths();

  /** The number of stored paths for a profile. Cheaper than {@code pathsFor(profileType).size()}. */
  int calculateNumberOfTransferPaths(TransferProfileType profileType);
}
