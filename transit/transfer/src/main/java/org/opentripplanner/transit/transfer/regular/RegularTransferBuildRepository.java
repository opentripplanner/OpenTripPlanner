package org.opentripplanner.transit.transfer.regular;

import java.io.Serializable;
import java.util.Map;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;

/**
 * The regular-transfer path templates generated at graph build, keyed by
 * {@code (userPreferences, fromStop, toStop)}, and serialized with the graph. Written by the
 * {@link TransferGenerator}.
 * <p>
 * This repository is only used at graph build, it is not the runtime repository. When the
 * application starts, it is converted into the {@link RegularTransferRepository}, which is then
 * only updated inside the transaction framework - see {@link #createRuntimeRepository}. The
 * conversion seals this repository: writing to it afterward throws an
 * {@link IllegalStateException}.
 *
 * @param <P> the street path type
 */
public interface RegularTransferBuildRepository<P> extends Serializable {
  /**
   * Create the first runtime {@link RegularTransferRepository} from the paths generated at graph
   * build, and seal the build repository: from now on regular transfers can only be updated
   * through the runtime repositories. They share the paths of the build repository instead of
   * copying them; the seal keeps them unchanged, writes to the build repository after this throw.
   */
  RegularTransferRepository<P> createRuntimeRepository();

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

  /** Whether {@code fromStop} has at least one outgoing transfer, under any user preferences. */
  boolean hasTransfersFrom(int fromStop);

  /** The number of paths stored under exactly the given user preferences. */
  int calculateNumberOfTransferPaths(AbstractUserPreferences<?> userPreferences);
}
