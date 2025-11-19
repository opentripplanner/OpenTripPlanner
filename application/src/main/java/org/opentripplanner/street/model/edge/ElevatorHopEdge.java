package org.opentripplanner.street.model.edge;

import java.time.Duration;
import java.util.Optional;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.transit.model.basic.Accessibility;

/**
 * A relatively low cost edge for travelling one level in an elevator.
 *
 * @author mattwigway
 */
public class ElevatorHopEdge extends Edge implements ElevatorEdge, WheelchairTraversalInformation {

  private static final double DEFAULT_LEVELS = 1;
  private static final int DEFAULT_TRAVEL_TIME = 0;

  private final StreetTraversalPermission permission;

  private final Accessibility wheelchairAccessibility;

  private final double levels;
  private final int travelTime;

  private ElevatorHopEdge(
    Vertex from,
    Vertex to,
    StreetTraversalPermission permission,
    Accessibility wheelchairAccessibility,
    double levels,
    int travelTime
  ) {
    super(from, to);
    this.permission = permission;
    this.wheelchairAccessibility = wheelchairAccessibility;
    this.levels = levels;
    this.travelTime = travelTime;
  }

  private ElevatorHopEdge(
    Vertex from,
    Vertex to,
    StreetTraversalPermission permission,
    Accessibility wheelchairAccessibility
  ) {
    this(from, to, permission, wheelchairAccessibility, DEFAULT_LEVELS, DEFAULT_TRAVEL_TIME);
  }

  public static void bidirectional(
    Vertex from,
    Vertex to,
    StreetTraversalPermission permission,
    Accessibility wheelchairBoarding,
    int levels,
    int travelTime
  ) {
    createElevatorHopEdge(from, to, permission, wheelchairBoarding, levels, travelTime);
    createElevatorHopEdge(to, from, permission, wheelchairBoarding, levels, travelTime);
  }

  public static void bidirectional(
    Vertex from,
    Vertex to,
    StreetTraversalPermission permission,
    Accessibility wheelchairBoarding
  ) {
    createElevatorHopEdge(from, to, permission, wheelchairBoarding);
    createElevatorHopEdge(to, from, permission, wheelchairBoarding);
  }

  public static ElevatorHopEdge createElevatorHopEdge(
    Vertex from,
    Vertex to,
    StreetTraversalPermission permission,
    Accessibility wheelchairAccessibility,
    double levels,
    int travelTime
  ) {
    return connectToGraph(
      new ElevatorHopEdge(from, to, permission, wheelchairAccessibility, levels, travelTime)
    );
  }

  public static ElevatorHopEdge createElevatorHopEdge(
    Vertex from,
    Vertex to,
    StreetTraversalPermission permission,
    Accessibility wheelchairAccessibility
  ) {
    return connectToGraph(new ElevatorHopEdge(from, to, permission, wheelchairAccessibility));
  }

  public StreetTraversalPermission getPermission() {
    return permission;
  }

  /**
   * The number of levels that the elevator travels
   */
  public double getLevels() {
    return levels;
  }

  /**
   * Returns the travel time of the elevator.
   * If travelTime is 0, returns an empty Optional.
   */
  public Optional<Duration> getTravelTime() {
    return travelTime > 0 ? Optional.of(Duration.ofSeconds(travelTime)) : Optional.empty();
  }

  @Override
  public State[] traverse(State s0) {
    var request = s0.getRequest();

    StateEditor s1 = createEditorForDrivingOrWalking(s0, this);

    if (s0.getRequest().wheelchairEnabled()) {
      if (
        wheelchairAccessibility != Accessibility.POSSIBLE &&
        request.wheelchair().elevator().onlyConsiderAccessible()
      ) {
        return State.empty();
      } else if (wheelchairAccessibility == Accessibility.NO_INFORMATION) {
        s1.incrementWeight(request.wheelchair().elevator().unknownCost());
      } else if (wheelchairAccessibility == Accessibility.NOT_POSSIBLE) {
        s1.incrementWeight(request.wheelchair().elevator().inaccessibleCost());
      }
    }

    TraverseMode mode = s0.currentMode();

    if (mode == TraverseMode.WALK && !permission.allows(StreetTraversalPermission.PEDESTRIAN)) {
      return State.empty();
    }

    if (mode == TraverseMode.BICYCLE && !permission.allows(StreetTraversalPermission.BICYCLE)) {
      return State.empty();
    }
    // there are elevators which allow cars
    if (mode == TraverseMode.CAR && !permission.allows(StreetTraversalPermission.CAR)) {
      return State.empty();
    }

    s1.incrementWeight(
      this.travelTime > 0 ? this.travelTime : (request.elevator().hopCost() * this.levels)
    );
    int seconds = this.travelTime > 0
      ? this.travelTime
      : (int) (request.elevator().hopTime() * this.levels);
    s1.incrementTimeInSeconds(seconds);
    return s1.makeStateArray();
  }

  @Override
  public I18NString getName() {
    return null;
  }

  @Override
  public boolean isWheelchairAccessible() {
    return wheelchairAccessibility == Accessibility.POSSIBLE;
  }
}
