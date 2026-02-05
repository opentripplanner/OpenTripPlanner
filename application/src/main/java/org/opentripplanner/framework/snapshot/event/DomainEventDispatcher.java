package org.opentripplanner.framework.snapshot.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.framework.snapshot.domain.DomainEvent;
import org.opentripplanner.framework.snapshot.domain.world.TransitWorld;

public class DomainEventDispatcher {

  private final Map<Class<?>, List<DomainEventHandler<?>>> handlers = new HashMap<>();
  private boolean frozen = false;

  public void register(DomainEventHandler<?> handler) {
    if (frozen) {
      throw new IllegalStateException("Cannot register after startup");
    }
    handlers
      .computeIfAbsent(handler.eventType(), k -> new ArrayList<>())
      .add(handler);
  }

  public void freeze() {
    frozen = true;
  }

  public <E extends DomainEvent> void dispatch(List<E> events, TransitWorld transitWorld) {
    if (!frozen) {
      throw new IllegalStateException("Dispatcher not frozen");
    }
    for (var event : events) {
      for (var genericHandler : handlers.getOrDefault(event.getClass(), List.of())) {
        DomainEventHandler<E> handler = (DomainEventHandler<E>) genericHandler;
        handler.handle(event, transitWorld);
      }
    }
  }
}
