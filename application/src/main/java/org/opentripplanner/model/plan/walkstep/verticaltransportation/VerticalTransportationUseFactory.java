package org.opentripplanner.model.plan.walkstep.verticaltransportation;

import java.util.Optional;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.streetdetails.model.InclinedEdgeLevelInfo;
import org.opentripplanner.service.streetdetails.model.Level;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.path.step.VerticalTransportation;
import org.opentripplanner.street.model.vertex.OsmVertex;

/**
 * This factory is responsible for creating {@link VerticalTransportationUse} objects. This applies
 * to inclined edges and elevators.
 */
public class VerticalTransportationUseFactory {

  private final StreetDetailsService streetDetailsService;

  public VerticalTransportationUseFactory(StreetDetailsService streetDetailsService) {
    this.streetDetailsService = streetDetailsService;
  }

  public VerticalTransportationUse create(VerticalTransportation verticalTransportation) {
    return switch (verticalTransportation) {
      case VerticalTransportation.Elevator e -> createElevatorUse(e.boardEdge(), e.alightEdge());
      case VerticalTransportation.Escalator e -> createEscalatorUse(e.edge());
      case VerticalTransportation.Stairs s -> createStairsUse(s.edge());
    };
  }

  public ElevatorUse createElevatorUse(
    ElevatorBoardEdge elevatorBoardEdge,
    ElevatorAlightEdge elevatorAlightEdge
  ) {
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
    return edge.getFromVertex() instanceof OsmVertex fromVertex &&
      fromVertex.nodeId() == inclinedEdgeLevelInfo.lowerVertexInfo().osmNodeId()
      ? VerticalDirection.UP
      : VerticalDirection.DOWN;
  }
}
