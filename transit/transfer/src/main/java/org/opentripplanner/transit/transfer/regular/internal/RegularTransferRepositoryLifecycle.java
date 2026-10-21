package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Objects;
import org.opentripplanner.core.model.transaction.RepositoryLifecycle;
import org.opentripplanner.raptor.data.stop.StopIndex;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;

/**
 * Copy-on-write / freeze lifecycle for the regular-transfer repository. Each transaction that
 * writes transfers gets a new mutable repository initialized from the last committed snapshot, and
 * a new immutable snapshot is published when the transaction commits. Edits made to a repository
 * that is never frozen are simply discarded.
 * <p>
 * Every snapshot is created by {@link #freeze}, including the initial one. The lifecycle holds the
 * stop index and the path provider, which the snapshots need to derive the Raptor transfers from
 * their paths.
 * <p>
 * Neither direction copies the whole path map, see {@link TemplatePathsByUserProfile}.
 *
 * @param <P> the transfer path/template type
 */
public class RegularTransferRepositoryLifecycle<P> implements
  RepositoryLifecycle<RegularTransferRepositorySnapshot<P>, RegularTransferRepository<P>> {

  private final StopIndex stopIndex;
  private final TransferPathProvider<P, AbstractUserPreferences<?>> pathProvider;

  public RegularTransferRepositoryLifecycle(
    StopIndex stopIndex,
    TransferPathProvider<P, AbstractUserPreferences<?>> pathProvider
  ) {
    this.stopIndex = Objects.requireNonNull(stopIndex);
    this.pathProvider = Objects.requireNonNull(pathProvider);
  }

  @Override
  public RegularTransferRepository<P> copyOnWrite(RegularTransferRepositorySnapshot<P> snapshot) {
    // the cast is safe: all snapshots are created by freeze() below
    return ((DefaultRegularTransferRepositorySnapshot<P>) snapshot).copyOnWrite();
  }

  @Override
  public RegularTransferRepositorySnapshot<P> freeze(RegularTransferRepository<P> repository) {
    // the cast is safe: all repositories are created by copyOnWrite() above or by the build
    // repository
    var paths = ((DefaultRegularTransferRepository<P>) repository).freeze();
    return new DefaultRegularTransferRepositorySnapshot<>(paths, stopIndex, pathProvider);
  }
}
