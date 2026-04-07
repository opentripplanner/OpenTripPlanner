package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

/**
 * A temporary edge which links the origin / destination of a street search to the street graph.
 * Traversal is typically free, but arriving to a location on a rental vehicle may come with a cost.
 * Traversal can be limited with a {@link StreetTraversalPermission}.
 */
public class TemporaryFreeEdge extends FreeEdge implements TemporaryEdge, BikeWalkableEdge {

  private final StreetTraversalPermission permission;

  protected TemporaryFreeEdge(Vertex from, Vertex to, StreetTraversalPermission permission) {
    super(from, to);
    this.permission = permission;
  }

  public static TemporaryFreeEdge createTemporaryFreeEdge(
    Vertex from,
    Vertex to,
    StreetTraversalPermission permission
  ) {
    return connectToGraph(new TemporaryFreeEdge(from, to, permission));
  }

  public StreetTraversalPermission permission() {
    return permission;
  }

  @Override
  public State[] traverse(State s0) {
    StateEditor s1;
    if (!permission.allows(s0.currentMode())) {
      if (isWalkingBikeAllowed(s0)) {
        s1 = createEditor(s0, this, s0.currentMode(), true);
      } else {
        return State.empty();
      }
    } else {
      s1 = s0.edit(this);
    }

    // a small cost is added to prevent other searches going through this temporary edge
    s1.incrementWeight(1);
    s1.setBackMode(null);

    if (s0.isRentingVehicleFromStation() && s0.mayKeepRentedVehicleAtDestination()) {
      var rentalPreferences = s0.getRequest().rental(s0.getRequest().mode());
      if (rentalPreferences.allowArrivingInRentedVehicleAtDestination()) {
        s1.incrementWeight(
          rentalPreferences.arrivingInRentalVehicleAtDestinationCost().toSeconds()
        );
      }
    }

    return s1.makeStateArray();
  }

  private boolean isWalkingBikeAllowed(State s0) {
    return permission.allows(TraverseMode.WALK) && s0.currentMode() == TraverseMode.BICYCLE;
  }
}
