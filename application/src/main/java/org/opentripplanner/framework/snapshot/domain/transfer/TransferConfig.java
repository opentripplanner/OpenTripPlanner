package org.opentripplanner.framework.snapshot.domain.transfer;

import java.util.function.Supplier;
import org.opentripplanner.framework.snapshot.domain.transfer.repository.MutableTransferSnapshot;
import org.opentripplanner.framework.snapshot.domain.transfer.repository.ReadOnlyTransferSnapshot;
import org.opentripplanner.framework.snapshot.domain.transfer.repository.TransferSnapshotLifecycle;
import org.opentripplanner.framework.snapshot.event.EventDispatcher;
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

  public static NewStopHandler createApplicationScopedStopHandler(
    EventDispatcher eventDispatcher,
    Supplier<MutableTransferSnapshot> mutableTransferSnapshotSupplier
  ) {
    NewStopHandler newStopHandler = new NewStopHandler(mutableTransferSnapshotSupplier);
    eventDispatcher.register(newStopHandler);
    return newStopHandler;
  }
}
