package org.opentripplanner.framework.snapshot.persistence.snapshot;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.framework.snapshot.domain.DomainEvent;

public class DomainEventCollector {

  private final List<DomainEvent> events = new ArrayList<>();

  public void record(DomainEvent event) {
    events.add(event);
  }

  public List<DomainEvent> drain() {
    List<DomainEvent> copy = List.copyOf(events);
    events.clear();
    return copy;
  }
}
