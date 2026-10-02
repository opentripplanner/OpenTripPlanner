package org.opentripplanner.transit.transfer.regular;

import java.util.Map;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

/**
 * Mutable side of the regular-transfer repository. It holds every discovered {@code P} path
 * template, keyed by {@code (profileType, fromStop, toStop)}.
 * <p>
 * The repository is part of the transaction framework and only exists at runtime: write access is
 * only available through a {@code WriteContext} on the writer thread, requests read the immutable
 * {@link RegularTransferRepositorySnapshot} published on each commit. The initial snapshot is
 * created from the {@link RegularTransferBuildRepository}.
 *
 * @param <P> the transfer path/template type
 */
public interface RegularTransferRepository<P> {
  /** Store the path template for the given transfer, replacing any existing one. */
  void setPath(TransferProfileType profileType, int fromStop, int toStop, P path);

  /**
   * Store the path templates for all transfers from {@code fromStop}, keyed by {@code toStop}. Same
   * as calling {@link #setPath} for each entry: existing paths to other stops are kept. Prefer this
   * when writing many paths from the same stop, it is linear in the number of paths, while
   * repeated {@link #setPath} calls are quadratic. The given map is copied, not retained.
   */
  void setPaths(TransferProfileType profileType, int fromStop, Map<Integer, P> pathsByToStop);
}
