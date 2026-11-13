package org.opentripplanner.transfer.regular;

import java.util.Set;
import org.opentripplanner.core.model.event.DomainEvent;

/**
 * The set of closed facilities changed. A closable facility is a part of the street network that
 * a regular transfer path may depend on and that can be closed at runtime, for example an elevator
 * that is out of service, see {@code TransferPath#closableFacilityIds()}. A path that depends on a
 * closed facility can not be used.
 *
 * @param closedFacilityIds the ids of all the facilities that are closed now, not only the ones
 *                          that changed. Every facility that is not in the set is open. The set is
 *                          copied.
 */
public record ClosedFacilitiesChanged(Set<String> closedFacilityIds) implements DomainEvent {
  public ClosedFacilitiesChanged {
    closedFacilityIds = Set.copyOf(closedFacilityIds);
  }
}
