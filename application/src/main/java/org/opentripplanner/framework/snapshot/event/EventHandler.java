package org.opentripplanner.framework.snapshot.event;

public interface EventHandler<T extends DomainEvent> {

  Class<T> eventType();

  void handle(T event);
}
