package org.opentripplanner.street.model.path.step;

import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;

/**
 * The vertical transportation equipment (elevator, escalator or stairs) used in a
 * {@link StreetStep}. Level information is not part of the street graph, so this only references
 * the edges traversed. It is up to the caller to look up the levels.
 */
public sealed interface VerticalTransportation {
  record Elevator(
    ElevatorBoardEdge boardEdge,
    ElevatorAlightEdge alightEdge
  ) implements VerticalTransportation {}

  record Escalator(Edge edge) implements VerticalTransportation {}

  record Stairs(Edge edge) implements VerticalTransportation {}
}
