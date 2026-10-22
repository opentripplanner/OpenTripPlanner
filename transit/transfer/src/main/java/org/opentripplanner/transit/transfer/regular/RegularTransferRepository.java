package org.opentripplanner.transit.transfer.regular;

import java.util.Map;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;

/**
 * Mutable side of the regular-transfer repository. It holds every {@code P} path template, keyed
 * by {@code (profile, fromStop, toStop)}: the paths generated at graph build, with the transfers
 * from some stops replaced at runtime.
 * <p>
 * The repository is part of the transaction framework and only exists at runtime: write access is
 * only available through a {@code WriteContext} on the writer thread, requests read the immutable
 * {@link RegularTransferRepositorySnapshot} published on each commit. The repository is created
 * from the {@link RegularTransferBuildRepository}.
 * <p>
 * The unit of change is all transfers from one stop under one profile. The caller computes the
 * complete set of transfers a stop should have; the repository does not merge, and it does not
 * track why a stop's transfers were replaced. A profile is the exact user preferences the paths
 * were generated under at graph build.
 *
 * @param <P> the street path type
 */
public interface RegularTransferRepository<P> {
  /**
   * Replace all transfers from {@code fromStop} under {@code profile} with the given paths, keyed
   * by {@code toStop}. An empty map removes all transfers from the stop. The given map is copied,
   * not retained.
   *
   * @throws IllegalArgumentException if no paths were generated for {@code profile} at graph build
   */
  void replaceTransfersFrom(
    AbstractUserPreferences<?> profile,
    int fromStop,
    Map<Integer, P> pathsByToStop
  );

  /**
   * Undo {@link #replaceTransfersFrom}: the transfers from {@code fromStop} under
   * {@code profile} are the ones generated at graph build again. Does nothing if they were not
   * replaced.
   *
   * @throws IllegalArgumentException if no paths were generated for {@code profile} at graph build
   */
  void resetTransfersFrom(AbstractUserPreferences<?> profile, int fromStop);
}
