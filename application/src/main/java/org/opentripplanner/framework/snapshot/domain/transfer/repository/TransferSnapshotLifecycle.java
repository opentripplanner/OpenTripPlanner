package org.opentripplanner.framework.snapshot.domain.transfer.repository;

import org.opentripplanner.framework.snapshot.transaction.RepositoryLifecycle;

public class TransferSnapshotLifecycle
  implements RepositoryLifecycle<ReadOnlyTransferSnapshot, MutableTransferSnapshot> {

  @Override
  public MutableTransferSnapshot copyOnWrite(ReadOnlyTransferSnapshot readOnlySnapshot) {
    return new MutableTransferSnapshot(readOnlySnapshot.getNumberOfRecalculations());
  }

  @Override
  public ReadOnlyTransferSnapshot freeze(MutableTransferSnapshot mutableSnapshot) {
    return new ReadOnlyTransferSnapshot(mutableSnapshot.getNumberOfRecalculations());
  }
}
