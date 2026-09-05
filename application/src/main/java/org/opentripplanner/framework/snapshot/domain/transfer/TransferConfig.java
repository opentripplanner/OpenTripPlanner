package org.opentripplanner.framework.snapshot.domain.transfer;

import org.opentripplanner.framework.snapshot.domain.transfer.repository.MutableTransferSnapshot;
import org.opentripplanner.framework.snapshot.domain.transfer.repository.ReadOnlyTransferSnapshot;
import org.opentripplanner.framework.snapshot.domain.transfer.repository.TransferSnapshotLifecycle;
import org.opentripplanner.framework.snapshot.transaction.RepositoryHandle;
import org.opentripplanner.framework.snapshot.transaction.RepositoryRegistry;

public class TransferConfig {

  public static RepositoryHandle<ReadOnlyTransferSnapshot, MutableTransferSnapshot> createRepo(
    RepositoryRegistry repositoryRegistry
  ) {
    return repositoryRegistry.register(
      new ReadOnlyTransferSnapshot(0),
      new TransferSnapshotLifecycle()
    );
  }
}
