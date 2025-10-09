package org.opentripplanner.street.model.edge;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.transit.model.site.PathwayMode;

/**
 * A walking pathway as described in GTFS
 */
public class PathwayEdge extends Edge implements BikeWalkableEdge, WheelchairTraversalInformation {

  public static final I18NString DEFAULT_NAME = new NonLocalizedString("pathway");

  @Nullable
  private final I18NString signpostedAs;

  private final int traversalTime;
  private final double distance;
  private final int steps;
  private final double slope;
  private final PathwayMode mode;

  private final boolean wheelchairAccessible;

  private PathwayEdge(
    Vertex fromv,
    Vertex tov,
    @Nullable I18NString signpostedAs,
    int traversalTime,
    double distance,
    int steps,
    double slope,
    boolean wheelchairAccessible,
    PathwayMode mode
  ) {
    super(fromv, tov);
    this.signpostedAs = signpostedAs;
    this.traversalTime = traversalTime;
    this.steps = steps;
    this.slope = slope;
    this.wheelchairAccessible = wheelchairAccessible;
    this.distance = distance;
    this.mode = mode;
  }

  /**
   * {@link #createLowCostPathwayEdge(Vertex, Vertex, boolean, PathwayMode)}
   */
  public static PathwayEdge createLowCostPathwayEdge(Vertex fromV, Vertex toV, PathwayMode mode) {
    return PathwayEdge.createLowCostPathwayEdge(fromV, toV, true, mode);
  }

  /**
   * Create a PathwayEdge that doesn't have a traversal time, distance or steps.
   * <p>
   * These are for edges which have an implied cost of almost zero just like a FreeEdge has.
   */
  public static PathwayEdge createLowCostPathwayEdge(
    Vertex fromV,
    Vertex toV,
    boolean wheelchairAccessible,
    PathwayMode mode
  ) {
    return createPathwayEdge(fromV, toV, null, 0, 0, 0, 0, wheelchairAccessible, mode);
  }

  public static PathwayEdge createPathwayEdge(
    Vertex fromv,
    Vertex tov,
    I18NString signpostedAs,
    int traversalTime,
    double distance,
    int steps,
    double slope,
    boolean wheelchairAccessible,
    PathwayMode mode
  ) {
    return connectToGraph(
      new PathwayEdge(
        fromv,
        tov,
        signpostedAs,
        traversalTime,
        distance,
        steps,
        slope,
        wheelchairAccessible,
        mode
      )
    );
  }

  @Override
  public State[] traverse(State s0) {
    StateEditor s1 = createEditorForWalking(s0, this);
    if (s1 == null) {
      return State.empty();
    }

    RoutingPreferences preferences = s0.getPreferences();

    /* TODO: Consider mode, so that passing through multiple fare gates is not possible */
    long time_ms = 1000L * traversalTime;

    if (time_ms == 0) {
      if (distance > 0) {
        time_ms = (long) ((1000.0 * distance) / preferences.walk().speed());
      } else if (isStairs()) {
        // 1 step corresponds to 20cm, doubling that to compensate for elevation;
        time_ms = (long) ((1000.0 * 0.4 * Math.abs(steps)) / preferences.walk().speed());
      }
    }

    if (time_ms > 0) {
      double weight = time_ms / 1000.0;
      if (s0.getRequest().wheelchair()) {
        weight *= StreetEdgeReluctanceCalculator.computeWheelchairReluctance(
          preferences,
          slope,
          wheelchairAccessible,
          isStairs()
        );
      } else {
        weight *= StreetEdgeReluctanceCalculator.computeReluctance(
          preferences,
          TraverseMode.WALK,
          s0.currentMode() == TraverseMode.BICYCLE,
          isStairs()
        );
      }
      s1.incrementTimeInMilliseconds(time_ms);
      s1.incrementWeight(weight);
    } else {
      // elevators often don't have a traversal time, distance or steps, so we need to add
      // _some_ cost. the real cost is added in ElevatorHopEdge.
      // adding a cost of 1 is analogous to FreeEdge
      s1.incrementWeight(1);
    }

    return s1.makeStateArray();
  }

  /**
   * Return the sign to follow when traversing the pathway. An empty optional means that this
   * pathway does not have "signposted at" information.
   */
  public Optional<I18NString> signpostedAs() {
    return Optional.ofNullable(signpostedAs);
  }

  @Override
  public I18NString getName() {
    return Objects.requireNonNullElse(signpostedAs, DEFAULT_NAME);
  }

  @Override
  public boolean nameIsDerived() {
    return signpostedAs == null;
  }

  public LineString getGeometry() {
    Coordinate[] coordinates = new Coordinate[] {
      getFromVertex().getCoordinate(),
      getToVertex().getCoordinate(),
    };
    return GeometryUtils.getGeometryFactory().createLineString(coordinates);
  }

  public double getDistanceMeters() {
    return this.distance;
  }

  @Override
  public double getEffectiveWalkDistance() {
    if (traversalTime > 0) {
      return 0;
    } else {
      return distance;
    }
  }

  public int getSteps() {
    return steps;
  }

  @Override
  public boolean isWheelchairAccessible() {
    return wheelchairAccessible;
  }

  public PathwayMode getMode() {
    return mode;
  }

  private boolean isStairs() {
    return steps > 0;
  }
}
