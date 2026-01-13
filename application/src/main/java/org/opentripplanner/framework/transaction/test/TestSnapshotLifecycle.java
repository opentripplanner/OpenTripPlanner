package org.opentripplanner.framework.transaction.test;

import org.opentripplanner.framework.transaction.RepositoryLifecycle;

class TestSnapshotLifecycle implements RepositoryLifecycle<ReadOnlyTestSnapshot, MutableTestSnapshot> {
  @Override
  public MutableTestSnapshot copyOnWrite(ReadOnlyTestSnapshot readOnlyRepository) {
    return new MutableTestSnapshot(readOnlyRepository.state());
  }

  @Override
  public ReadOnlyTestSnapshot freeze(MutableTestSnapshot editableReopsitory) {
    return new ReadOnlyTestSnapshot(editableReopsitory.state());
  }
}
