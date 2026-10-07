package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Map;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

/**
 * Default {@link RegularTransferRepository}. A new instance is created for each transaction that
 * writes transfers - initialized from the last committed snapshot - and is only accessed on the
 * single writer thread. Neither creating it nor {@link #freeze()} copies the whole path map, see
 * {@link TransferPathMap}.
 *
 * @param <P> the transfer path/template type
 */
class DefaultRegularTransferRepository<P> implements RegularTransferRepository<P> {

  private final TransferPathMap<P> paths;

  /** Create an empty repository. */
  DefaultRegularTransferRepository() {
    this(new TransferPathMap<>());
  }

  /** Create a repository initialized with the paths of a snapshot. */
  DefaultRegularTransferRepository(TransferPathMap<P> paths) {
    this.paths = paths.copy();
  }

  @Override
  public void setPath(TransferProfileType profileType, int fromStop, int toStop, P path) {
    setPaths(profileType, fromStop, Map.of(toStop, path));
  }

  @Override
  public void setPaths(
    TransferProfileType profileType,
    int fromStop,
    Map<Integer, P> pathsByToStop
  ) {
    paths.setPaths(profileType, fromStop, pathsByToStop);
  }

  /**
   * Publish the current state as an immutable snapshot. Only the lifecycle should have access to
   * this, hence the package local access.
   */
  DefaultRegularTransferRepositorySnapshot<P> freeze() {
    return new DefaultRegularTransferRepositorySnapshot<>(paths.copy());
  }
}
