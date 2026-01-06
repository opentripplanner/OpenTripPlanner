package org.opentripplanner.model.plan.walkstep.verticaltransportation;

import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.streetdetails.model.InclinedEdgeLevelInfo;
import org.opentripplanner.service.streetdetails.model.Level;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.search.state.State;

/**
 * This factory is responsible for creating {@link VerticalTransportationUse} objects.
 * This applies to inclined edges and elevators.
 */
public class VerticalTransportationUseFactory {

  private final StreetDetailsService streetDetailsService;

  public VerticalTransportationUseFactory(StreetDetailsService streetDetailsService) {
    this.streetDetailsService = streetDetailsService;
  }

  public ElevatorUse createElevatorUse(State backState, ElevatorAlightEdge elevatorAlightEdge) {
    ElevatorBoardEdge elevatorBoardEdge = findElevatorBoardEdge(backState);
    if (elevatorBoardEdge == null) {
      throw new IllegalStateException(
        "An ElevatorAlightEdge was reached without first traversing an ElevatorBoardEdge"
      );
    }

    Optional<Level> boardEdgeLevelOptional = streetDetailsService.findHorizontalEdgeLevelInfo(
      elevatorBoardEdge
    );
    Optional<Level> alightEdgeLevelOptional = streetDetailsService.findHorizontalEdgeLevelInfo(
      elevatorAlightEdge
    );
    if (boardEdgeLevelOptional.isPresent() && alightEdgeLevelOptional.isPresent()) {
      Level boardEdgeLevel = boardEdgeLevelOptional.get();
      Level alightEdgeLevel = alightEdgeLevelOptional.get();
      VerticalDirection verticalDirection = VerticalDirection.UNKNOWN;
      if (boardEdgeLevel.level() > alightEdgeLevel.level()) {
        verticalDirection = VerticalDirection.DOWN;
      } else if (boardEdgeLevel.level() < alightEdgeLevel.level()) {
        verticalDirection = VerticalDirection.UP;
      }
      return new ElevatorUse(boardEdgeLevel, alightEdgeLevel, verticalDirection);
    } else if (boardEdgeLevelOptional.isPresent()) {
      return new ElevatorUse(boardEdgeLevelOptional.get(), null, VerticalDirection.UNKNOWN);
    } else if (alightEdgeLevelOptional.isPresent()) {
      return new ElevatorUse(null, alightEdgeLevelOptional.get(), VerticalDirection.UNKNOWN);
    }
    return new ElevatorUse(null, null, VerticalDirection.UNKNOWN);
  }

  public EscalatorUse createEscalatorUse(Edge edge) {
    Optional<InclinedEdgeLevelInfo> inclinedEdgeLevelInfoOptional =
      streetDetailsService.findInclinedEdgeLevelInfo(edge);
    if (inclinedEdgeLevelInfoOptional.isEmpty()) {
      return new EscalatorUse(null, null, VerticalDirection.UNKNOWN);
    }
    InclinedEdgeLevelInfo inclinedEdgeLevelInfo = inclinedEdgeLevelInfoOptional.get();

    VerticalDirection verticalDirection = getInclinedEdgeVerticalDirection(
      edge,
      inclinedEdgeLevelInfo
    );
    if (verticalDirection == VerticalDirection.UP) {
      return new EscalatorUse(
        inclinedEdgeLevelInfo.lowerVertexInfo().level(),
        inclinedEdgeLevelInfo.upperVertexInfo().level(),
        verticalDirection
      );
    } else {
      return new EscalatorUse(
        inclinedEdgeLevelInfo.upperVertexInfo().level(),
        inclinedEdgeLevelInfo.lowerVertexInfo().level(),
        verticalDirection
      );
    }
  }

  public StairsUse createStairsUse(Edge edge) {
    Optional<InclinedEdgeLevelInfo> inclinedEdgeLevelInfoOptional =
      streetDetailsService.findInclinedEdgeLevelInfo(edge);
    if (inclinedEdgeLevelInfoOptional.isEmpty()) {
      return new StairsUse(null, null, VerticalDirection.UNKNOWN);
    }
    InclinedEdgeLevelInfo inclinedEdgeLevelInfo = inclinedEdgeLevelInfoOptional.get();

    VerticalDirection verticalDirection = getInclinedEdgeVerticalDirection(
      edge,
      inclinedEdgeLevelInfo
    );
    if (verticalDirection == VerticalDirection.UP) {
      return new StairsUse(
        inclinedEdgeLevelInfo.lowerVertexInfo().level(),
        inclinedEdgeLevelInfo.upperVertexInfo().level(),
        verticalDirection
      );
    } else {
      return new StairsUse(
        inclinedEdgeLevelInfo.upperVertexInfo().level(),
        inclinedEdgeLevelInfo.lowerVertexInfo().level(),
        verticalDirection
      );
    }
  }

  private VerticalDirection getInclinedEdgeVerticalDirection(
    Edge edge,
    InclinedEdgeLevelInfo inclinedEdgeLevelInfo
  ) {
    return (
        edge.getFromVertex() instanceof OsmVertex fromVertex &&
        fromVertex.nodeId() == inclinedEdgeLevelInfo.lowerVertexInfo().osmNodeId()
      )
      ? VerticalDirection.UP
      : VerticalDirection.DOWN;
  }

  /**
   * Find the ElevatorBoardEdge that was used from the backState of an ElevatorAlightEdge.
   * This function should never return null unless the graph is broken.
   */
  @Nullable
  private ElevatorBoardEdge findElevatorBoardEdge(State backState) {
    // The initial value is the first possible state that can be the ElevatorBoardEdge.
    State currentState = backState.getBackState();
    while (currentState != null) {
      if (currentState.getBackEdge() instanceof ElevatorBoardEdge elevatorBoardEdge) {
        return elevatorBoardEdge;
      }
      currentState = currentState.getBackState();
    }
    return null;
  }
}
