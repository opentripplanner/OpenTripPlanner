package org.opentripplanner.framework.snapshot.event;

import org.opentripplanner.framework.snapshot.domain.DomainEvent;

public sealed interface DomainEventHandler<T extends DomainEvent>
  permits DomainEventHandlerTransfers, DomainEventHandlerTimetable {

  Class<T> eventType();

}
