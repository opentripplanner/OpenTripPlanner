package org.opentripplanner.framework.snapshot.persistence.snapshot;

import java.util.concurrent.atomic.AtomicReference;
import org.opentripplanner.framework.snapshot.persistence.snapshot.world.ImmutableTransitWorld;

public class SnapshotStore {

  private final AtomicReference<ImmutableTransitWorld> current;

  public SnapshotStore(ImmutableTransitWorld initial) {
    this.current = new AtomicReference<>(initial);
  }

  public ImmutableTransitWorld current() {
    return current.get();
  }

  public void commit(ImmutableTransitWorld newWorld) {
    current.set(newWorld);
  }
}
