package org.opentripplanner.transit.transfer.regular;

import java.util.List;
import java.util.Map;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot.StoredPath;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

/**
 * Mutable side of the regular-transfer repository. It holds every discovered {@code P} path
 * template, keyed by {@code (profileType, fromStop, toStop)}.
 * <p>
 * The repository is part of the transaction framework, requests never see it - they read the
 * immutable {@link RegularTransferRepositorySnapshot} published on each commit instead. At
 * graph-build time {@link TransferGenerator} writes to it directly, and the graph builder reads it
 * for reporting; at runtime write access is only available through a {@code WriteContext} on the
 * writer thread.
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

  /**
   * Whether {@code fromStop} has at least one outgoing transfer, in any profile. Used at graph
   * build to flag stops that could not be linked to any other stop.
   */
  boolean hasTransfersFrom(int fromStop);

  /**
   * Every stored {@code (fromStop, toStop, path)} for a profile. Empty if it has none. Used at
   * graph build for reporting, requests use {@link RegularTransferRepositorySnapshot#pathsFor}.
   */
  List<StoredPath<P>> pathsFor(TransferProfileType profileType);

  /** The number of stored paths, in all profiles. */
  int calculateNumberOfTransferPaths();
}
