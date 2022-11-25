package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.vertex.TemporaryVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

public class TemporaryFreeEdge extends FreeEdge implements TemporaryEdge {

  public TemporaryFreeEdge(TemporaryVertex from, Vertex to) {
    super((Vertex) from, to);
    if (from.isEndVertex()) {
      throw new IllegalStateException("A temporary edge is directed away from an end vertex");
    }
  }

  public TemporaryFreeEdge(Vertex from, TemporaryVertex to) {
    super(from, (Vertex) to);
    if (!to.isEndVertex()) {
      throw new IllegalStateException("A temporary edge is directed towards a start vertex");
    }
  }

  @Override
  public String toString() {
    return "Temporary" + super.toString();
  }

  @Override
  public State traverse(State s0) {
    StateEditor s1 = s0.edit(this);
    s1.incrementWeight(1);
    s1.setBackMode(null);

    if (
      s0.isRentingVehicleFromStation() &&
      s0.mayKeepRentedVehicleAtDestination() &&
      s0.getRequest().rental().allowArrivingInRentedVehicleAtDestination()
    ) {
      s1.incrementWeight(s0.getPreferences().rental().arrivingInRentalVehicleAtDestinationCost());
    }

    return s1.makeState();
  }
}
