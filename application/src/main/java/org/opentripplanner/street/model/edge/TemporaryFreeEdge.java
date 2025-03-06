package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.vertex.TemporaryVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

public class TemporaryFreeEdge extends FreeEdge implements TemporaryEdge {

  private TemporaryFreeEdge(TemporaryVertex from, Vertex to) {
    super((Vertex) from, to);
  }

  private TemporaryFreeEdge(Vertex from, TemporaryVertex to) {
    super(from, (Vertex) to);
  }

  public static TemporaryFreeEdge createTemporaryFreeEdge(TemporaryVertex from, Vertex to) {
    return connectToGraph(new TemporaryFreeEdge(from, to));
  }

  public static TemporaryFreeEdge createTemporaryFreeEdge(Vertex from, TemporaryVertex to) {
    return connectToGraph(new TemporaryFreeEdge(from, to));
  }

  @Override
  public State[] traverse(State s0) {
    StateEditor s1 = s0.edit(this);
    s1.incrementWeight(1);
    s1.setBackMode(null);

    if (s0.isRentingVehicleFromStation() && s0.mayKeepRentedVehicleAtDestination()) {
      var rentalPreferences = s0.getPreferences().rental(s0.getRequest().mode());
      if (rentalPreferences.allowArrivingInRentedVehicleAtDestination()) {
        s1.incrementWeight(
          rentalPreferences.arrivingInRentalVehicleAtDestinationCost().toSeconds()
        );
      }
    }

    return s1.makeStateArray();
  }
}
