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
  /**
   * @deprecated This type exposes the internal street model edges. It is temporary and will be
   * removed in a future refactoring.
   */
  @Deprecated
  record Elevator(
    ElevatorBoardEdge boardEdge,
    ElevatorAlightEdge alightEdge
  ) implements VerticalTransportation {}

  /**
   * @deprecated This type exposes the internal street model {@link Edge}. It is temporary and will
   * be removed in a future refactoring.
   */
  @Deprecated
  record Escalator(Edge edge) implements VerticalTransportation {}

  /**
   * @deprecated This type exposes the internal street model {@link Edge}. It is temporary and will
   * be removed in a future refactoring.
   */
  @Deprecated
  record Stairs(Edge edge) implements VerticalTransportation {}
}
