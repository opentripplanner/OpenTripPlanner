package org.opentripplanner.transit.transfer.regular.internal;

import org.opentripplanner.core.model.transaction.RepositoryLifecycle;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;

/**
 * Copy-on-write / freeze lifecycle for the regular-transfer repository. Each transaction that
 * writes transfers gets a new mutable repository initialized from the last committed snapshot, and
 * a new immutable snapshot is published when the transaction commits. Edits made to a repository
 * that is never frozen are simply discarded, so a failed transaction rolls back cleanly.
 * <p>
 * Neither direction copies the whole path map, see {@link DefaultRegularTransferRepository}.
 *
 * @param <P> the transfer path/template type
 */
public class RegularTransferRepositoryLifecycle<P> implements
  RepositoryLifecycle<RegularTransferRepositorySnapshot<P>, RegularTransferRepository<P>> {

  @Override
  public RegularTransferRepository<P> copyOnWrite(RegularTransferRepositorySnapshot<P> snapshot) {
    // the cast is safe: all snapshots are created by freeze() below
    return ((DefaultRegularTransferRepositorySnapshot<P>) snapshot).copyOnWrite();
  }

  @Override
  public RegularTransferRepositorySnapshot<P> freeze(RegularTransferRepository<P> repository) {
    // the cast is safe: all repositories are created by copyOnWrite() above or at graph build
    return ((DefaultRegularTransferRepository<P>) repository).freeze();
  }
}
