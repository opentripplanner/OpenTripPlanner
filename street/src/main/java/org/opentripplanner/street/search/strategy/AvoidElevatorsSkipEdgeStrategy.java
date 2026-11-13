package org.opentripplanner.street.search.strategy;

import java.util.Set;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Skips the {@link ElevatorHopEdge}s with an id in the given set, so a street search does not use
 * these elevators. An elevator without an id can not be avoided.
 * <p>
 * Only add this strategy to a search if there is something to avoid, the check is done for every
 * edge of the search.
 */
public class AvoidElevatorsSkipEdgeStrategy implements SkipEdgeStrategy<State, Edge> {

  private final Set<String> elevatorIds;

  public AvoidElevatorsSkipEdgeStrategy(Set<String> elevatorIds) {
    this.elevatorIds = Set.copyOf(elevatorIds);
  }

  @Override
  public boolean shouldSkipEdge(State current, Edge edge) {
    return (
      edge instanceof ElevatorHopEdge elevator &&
      elevator.id().filter(elevatorIds::contains).isPresent()
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(AvoidElevatorsSkipEdgeStrategy.class)
      .addCol("elevatorIds", elevatorIds)
      .toString();
  }
}
