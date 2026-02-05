package org.opentripplanner.framework.snapshot.persistence.snapshot;

import java.util.function.Consumer;
import java.util.function.Function;
import org.opentripplanner.framework.snapshot.application.StateAccess;
import org.opentripplanner.framework.snapshot.application.TransactionalContext;
import org.opentripplanner.framework.snapshot.domain.world.TransitWorld;
import org.opentripplanner.framework.snapshot.persistence.world.MutableTransitWorld;
import org.opentripplanner.framework.snapshot.persistence.world.ImmutableTransitWorld;
import org.opentripplanner.framework.snapshot.event.DomainEventDispatcher;

public class DefaultStateAccess implements StateAccess {

  private final SnapshotStore snapshotStore;
  private final DomainEventDispatcher dispatcher;

  public DefaultStateAccess(SnapshotStore snapshotStore, DomainEventDispatcher dispatcher) {
    this.snapshotStore = snapshotStore;
    this.dispatcher = dispatcher;
  }

  public <R> R read(Function<TransitWorld, R> work) {
    ImmutableTransitWorld snapshot = snapshotStore.current();
    return work.apply(snapshot);
  }

  public void write(Consumer<TransactionalContext> work) {
    ImmutableTransitWorld base = snapshotStore.current();
    MutableTransitWorld working = MutableTransitWorld.from(base);
    DomainEventCollector events = new DomainEventCollector();

    DefaultTransactionalContext ctx = new DefaultTransactionalContext(working, events);

    work.accept(ctx);

    dispatcher.dispatch(events.drain(), working);
    snapshotStore.commit(working.freeze());
  }
}
