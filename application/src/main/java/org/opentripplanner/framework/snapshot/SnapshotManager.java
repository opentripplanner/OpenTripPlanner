package org.opentripplanner.framework.snapshot;

import java.util.concurrent.atomic.AtomicReference;

public class SnapshotManager {

  private final AtomicReference<TransitSnapshot> current;

  public SnapshotManager(AtomicReference<TransitSnapshot> initial) {
    this.current = initial;
  }

  public TransitSnapshot snapshot() {
    return current.get();
  }

  public void apply(SnapshotUpdate update) {
    while (true) {
      TransitSnapshot oldSnap = current.get();
      TransitSnapshot newSnap = update.apply(oldSnap);
      if (current.compareAndSet(oldSnap, newSnap)) {
        return;
      }
    }
  }
}
