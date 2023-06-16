package org.opentripplanner.street.model.edge;

import javax.annotation.Nonnull;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
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

  private final StreetTraversalPermission permission;

  private final Accessibility wheelchairAccessibility;

  private double levels = 1;
  private int travelTime = 0;

  public ElevatorHopEdge(
    Vertex from,
    Vertex to,
    StreetTraversalPermission permission,
    Accessibility wheelchairAccessibility,
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
    Accessibility wheelchairAccessibility
  ) {
    super(from, to);
    this.permission = permission;
    this.wheelchairAccessibility = wheelchairAccessibility;
  }

  public static void bidirectional(
    Vertex from,
    Vertex to,
    StreetTraversalPermission permission,
    Accessibility wheelchairBoarding,
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
    Accessibility wheelchairBoarding
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
  @Nonnull
  public State[] traverse(State s0) {
    RoutingPreferences preferences = s0.getPreferences();

    StateEditor s1 = createEditorForDrivingOrWalking(s0, this);

    if (s0.getRequest().wheelchair()) {
      if (
        wheelchairAccessibility != Accessibility.POSSIBLE &&
        preferences.wheelchair().elevator().onlyConsiderAccessible()
      ) {
        return State.empty();
      } else if (wheelchairAccessibility == Accessibility.NO_INFORMATION) {
        s1.incrementWeight(preferences.wheelchair().elevator().unknownCost());
      } else if (wheelchairAccessibility == Accessibility.NOT_POSSIBLE) {
        s1.incrementWeight(preferences.wheelchair().elevator().inaccessibleCost());
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
      this.travelTime > 0
        ? this.travelTime
        : (preferences.street().elevator().hopCost() * this.levels)
    );
    s1.incrementTimeInSeconds(
      this.travelTime > 0
        ? this.travelTime
        : (int) (preferences.street().elevator().hopTime() * this.levels)
    );
    return s1.makeStateArray();
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

  @Override
  public boolean isWheelchairAccessible() {
    return wheelchairAccessibility == Accessibility.POSSIBLE;
  }
}
