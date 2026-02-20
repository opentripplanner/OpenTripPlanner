package org.opentripplanner.framework.snapshot.persistence.snapshot;

import org.opentripplanner.framework.snapshot.application.TransactionalContext;
import org.opentripplanner.framework.snapshot.domain.timetable.TimetableRepo;
import org.opentripplanner.framework.snapshot.domain.transfer.TransferRepo;
import org.opentripplanner.framework.snapshot.domain.DomainEvent;
import org.opentripplanner.framework.snapshot.persistence.snapshot.world.MutableTransitWorld;

public class DefaultTransactionalContext implements TransactionalContext {

  private final MutableTransitWorld transitWorld;
  private final DomainEventCollector events;

  public DefaultTransactionalContext(MutableTransitWorld transitWorld, DomainEventCollector events) {
    this.transitWorld = transitWorld;
    this.events = events;
  }

  public TimetableRepo timetable() {
    return transitWorld.timetables();
  }

  public TransferRepo transfers() {
    return transitWorld.transfers();
  }

  public void publish(DomainEvent event) {
    events.record(event);
  }
}
