package org.opentripplanner.framework.snapshot.event;

import org.opentripplanner.framework.snapshot.domain.DomainEvent;
import org.opentripplanner.framework.snapshot.domain.world.TransitWorld;

public interface DomainEventHandler<T extends DomainEvent> {

  Class<T> eventType();

  void handle(T event, TransitWorld transitWorld);
}
