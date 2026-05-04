package org.opentripplanner.framework.snapshot.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventDispatcher {

  private final Map<Class<? extends DomainEvent>, List<EventHandler<? extends DomainEvent>>> eventHandlers = new HashMap<>();

  public void register(EventHandler<?> eventHandler) {
    eventHandlers.computeIfAbsent(eventHandler.eventType(), k -> new ArrayList<>())
      .add(eventHandler);
  }

  public void publish(DomainEvent event) {
    eventHandlers.getOrDefault(event.getClass(), Collections.emptyList())
      .forEach(handler -> dispatchTo(handler, event));
  }

  private <E extends DomainEvent> void dispatchTo(EventHandler<E> handler, DomainEvent event) {
    handler.handle((E) event);
  }
}
