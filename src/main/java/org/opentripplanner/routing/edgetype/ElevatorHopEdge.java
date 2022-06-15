package org.opentripplanner.routing.edgetype;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.WheelchairAccessibility;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.lang.ToStringBuilder;

/**
 * A relatively low cost edge for travelling one level in an elevator.
 *
 * @author mattwigway
 */
public class ElevatorHopEdge extends Edge implements ElevatorEdge {

  private static final long serialVersionUID = 3925814840369402222L;

  private final StreetTraversalPermission permission;

  private final WheelchairAccessibility wheelchairAccessibility;

  private double levels = 1;
  private int travelTime = 0;

  public ElevatorHopEdge(
    Vertex from,
    Vertex to,
    StreetTraversalPermission permission,
    WheelchairAccessibility wheelchairAccessibility,
    double levels,
    int travelTime
  ) {
    this(from, to, permission, wheelchairAccessibility);
    this.levels = levels;
    this.travelTime = travelTime;
  }

  public ElevatorHopEdge(
    Vertex from,
    Vertex to,
    StreetTraversalPermission permission,
    WheelchairAccessibility wheelchairAccessibility
  ) {
    super(from, to);
    this.permission = permission;
    this.wheelchairAccessibility = wheelchairAccessibility;
  }

  public static void bidirectional(
    Vertex from,
    Vertex to,
    StreetTraversalPermission permission,
    WheelchairAccessibility wheelchairBoarding,
    int levels,
    int travelTime
  ) {
    new ElevatorHopEdge(from, to, permission, wheelchairBoarding, levels, travelTime);
    new ElevatorHopEdge(to, from, permission, wheelchairBoarding, levels, travelTime);
  }

  public static void bidirectional(
    Vertex from,
    Vertex to,
    StreetTraversalPermission permission,
    WheelchairAccessibility wheelchairBoarding
  ) {
    new ElevatorHopEdge(from, to, permission, wheelchairBoarding);
    new ElevatorHopEdge(to, from, permission, wheelchairBoarding);
  }

  public StreetTraversalPermission getPermission() {
    return permission;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass()).addObj("from", fromv).addObj("to", tov).toString();
  }

  @Override
  public State traverse(State s0) {
    RoutingRequest request = s0.getOptions();

    StateEditor s1 = createEditorForDrivingOrWalking(s0, this);

    if (request.wheelchairAccessibility.enabled()) {
      if (
        wheelchairAccessibility != WheelchairAccessibility.POSSIBLE &&
        request.wheelchairAccessibility.elevator().onlyConsiderAccessible()
      ) {
        return null;
      } else if (wheelchairAccessibility == WheelchairAccessibility.NO_INFORMATION) {
        s1.incrementWeight(request.wheelchairAccessibility.elevator().unknownCost());
      } else if (wheelchairAccessibility == WheelchairAccessibility.NOT_POSSIBLE) {
        s1.incrementWeight(request.wheelchairAccessibility.elevator().inaccessibleCost());
      }
    }

    TraverseMode mode = s0.getNonTransitMode();

    if (mode == TraverseMode.WALK && !permission.allows(StreetTraversalPermission.PEDESTRIAN)) {
      return null;
    }

    if (mode == TraverseMode.BICYCLE && !permission.allows(StreetTraversalPermission.BICYCLE)) {
      return null;
    }
    // there are elevators which allow cars
    if (mode == TraverseMode.CAR && !permission.allows(StreetTraversalPermission.CAR)) {
      return null;
    }

    s1.incrementWeight(
      this.travelTime > 0 ? this.travelTime : (request.elevatorHopCost * this.levels)
    );
    s1.incrementTimeInSeconds(
      this.travelTime > 0 ? this.travelTime : (int) (request.elevatorHopTime * this.levels)
    );
    return s1.makeState();
  }

  @Override
  public I18NString getName() {
    return null;
  }

  @Override
  public LineString getGeometry() {
    return null;
  }

  @Override
  public double getDistanceMeters() {
    return 0;
  }

  public boolean isWheelchairAccessible() {
    return wheelchairAccessibility == WheelchairAccessibility.POSSIBLE;
  }
}
