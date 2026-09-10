package org.opentripplanner.service.transitalert.internal;

import org.opentripplanner.framework.transaction.api.RepositoryLifecycle;
import org.opentripplanner.service.transitalert.TransitAlertRepository;
import org.opentripplanner.service.transitalert.TransitAlertRepositorySnapshot;

/**
 * Copy-on-write / freeze lifecycle for the transit-alert repository. Each transaction that writes
 * alerts gets a new mutable repository initialized from the last committed snapshot, and a new
 * immutable snapshot is published when the transaction commits. Edits made to a repository that is
 * never frozen are simply discarded - this is what supports transaction rollback.
 * <p>
 * Incremental (differential) alert feeds still work: the mutable repository starts out with all
 * alerts of the last committed snapshot, so {@link TransitAlertRepository#addOrUpdateAlerts} and
 * {@link TransitAlertRepository#removeAlerts} build on everything committed before. The copy is
 * cheap because the alerts of a feed are only copied when that feed is written.
 */
public class TransitAlertRepositoryLifecycle
  implements RepositoryLifecycle<TransitAlertRepositorySnapshot, TransitAlertRepository>
{

  @Override
  public TransitAlertRepository copyOnWrite(TransitAlertRepositorySnapshot snapshot) {
    // the cast is safe: all snapshots are created by freeze() below
    return ((DefaultTransitAlertRepositorySnapshot) snapshot).copyOnWrite();
  }

  @Override
  public TransitAlertRepositorySnapshot freeze(TransitAlertRepository repository) {
    // the cast is safe: all repositories are created by copyOnWrite() above or by the module
    return ((DefaultTransitAlertRepository) repository).freeze();
  }
}
