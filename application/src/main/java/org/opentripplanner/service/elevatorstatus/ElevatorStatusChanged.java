package org.opentripplanner.service.elevatorstatus;

import java.util.Set;
import org.opentripplanner.core.model.event.DomainEvent;

/**
 * The elevators that are out of service changed. The event carries the complete state, not the
 * change: every elevator that is not in the set is in service.
 *
 * @param outOfServiceElevatorIds the ids of all the elevators that are out of service now, see
 *                                {@code ElevatorHopEdge#id()}. The set is copied.
 */
public record ElevatorStatusChanged(Set<String> outOfServiceElevatorIds) implements DomainEvent {
  public ElevatorStatusChanged {
    outOfServiceElevatorIds = Set.copyOf(outOfServiceElevatorIds);
  }
}
