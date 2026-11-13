package org.opentripplanner.service.elevatorstatus.model;

import java.util.Objects;

/**
 * Whether an elevator is in service. The status does not depend on the source of the data.
 *
 * @param elevatorId the id of the elevator in the street graph, see
 *                   {@code ElevatorHopEdge#id()}. The ids are unique across all sources.
 * @param inService  {@code false} if the elevator is out of service, and can not be used
 */
public record ElevatorStatus(String elevatorId, boolean inService) {
  public ElevatorStatus {
    Objects.requireNonNull(elevatorId);
  }

  public static ElevatorStatus inService(String elevatorId) {
    return new ElevatorStatus(elevatorId, true);
  }

  public static ElevatorStatus outOfService(String elevatorId) {
    return new ElevatorStatus(elevatorId, false);
  }
}
