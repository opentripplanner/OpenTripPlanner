package org.opentripplanner.street.model.edge;

import java.util.Objects;
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
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.PathwayMode;

/**
 * A walking pathway as described in GTFS
 */
public class PathwayEdge extends Edge implements BikeWalkableEdge, WheelchairTraversalInformation {

  public static final I18NString DEFAULT_NAME = new NonLocalizedString("pathway");
  private final I18NString name;
  private final int traversalTime;
  private final double distance;
  private final int steps;
  private final double slope;
  private final PathwayMode mode;

  private final boolean wheelchairAccessible;
  private final FeedScopedId id;

  public PathwayEdge(
    Vertex fromv,
    Vertex tov,
    FeedScopedId id,
    I18NString name,
    int traversalTime,
    double distance,
    int steps,
    double slope,
    boolean wheelchairAccessible,
    PathwayMode mode
  ) {
    super(fromv, tov);
    this.name = Objects.requireNonNullElse(name, DEFAULT_NAME);
    this.id = id;
    this.traversalTime = traversalTime;
    this.steps = steps;
    this.slope = slope;
    this.wheelchairAccessible = wheelchairAccessible;
    this.distance = distance;
    this.mode = mode;
  }

  /**
   * {@link PathwayEdge#lowCost(Vertex, Vertex, FeedScopedId, I18NString, boolean, PathwayMode)}
   */
  public static PathwayEdge lowCost(Vertex fromV, Vertex toV, I18NString name, PathwayMode mode) {
    return PathwayEdge.lowCost(fromV, toV, null, name, true, mode);
  }

  /**
   * Create a PathwayEdge that doesn't have a traversal time, distance or steps.
   * <p>
   * These are for edges which have an implied cost of almost zero just like a FreeEdge has.
   */
  public static PathwayEdge lowCost(
    Vertex fromV,
    Vertex toV,
    FeedScopedId id,
    I18NString name,
    boolean wheelchairAccessible,
    PathwayMode mode
  ) {
    return new PathwayEdge(fromV, toV, id, name, 0, 0, 0, 0, wheelchairAccessible, mode);
  }

  public State traverse(State s0) {
    StateEditor s1 = createEditorForWalking(s0, this);
    if (s1 == null) {
      return null;
    }

    RoutingPreferences preferences = s0.getPreferences();

    /* TODO: Consider mode, so that passing through multiple fare gates is not possible */
    int time = traversalTime;

    if (time == 0) {
      if (distance > 0) {
        time = (int) (distance * preferences.walk().speed());
      } else if (isStairs()) {
        // 1 step corresponds to 20cm, doubling that to compensate for elevation;
        time = (int) (0.4 * Math.abs(steps) * preferences.walk().speed());
      }
    }

    if (time > 0) {
      double weight = time;
      if (s0.getRequest().wheelchair()) {
        weight *=
          StreetEdgeReluctanceCalculator.computeWheelchairReluctance(
            preferences,
            slope,
            wheelchairAccessible,
            isStairs()
          );
      } else {
        weight *=
          StreetEdgeReluctanceCalculator.computeReluctance(
            preferences,
            TraverseMode.WALK,
            s0.getNonTransitMode() == TraverseMode.BICYCLE,
            isStairs()
          );
      }
      s1.incrementTimeInSeconds(time);
      s1.incrementWeight(weight);
    } else {
      // elevators often don't have a traversal time, distance or steps, so we need to add
      // _some_ cost. the real cost is added in ElevatorHopEdge.
      // adding a cost of 1 is analogous to FreeEdge
      s1.incrementWeight(1);
    }

    return s1.makeState();
  }

  @Override
  public I18NString getName() {
    return name;
  }

  @Override
  public boolean hasBogusName() {
    return name.equals(DEFAULT_NAME);
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

  @Override
  public int getDistanceIndependentTime() {
    return traversalTime;
  }

  public int getSteps() {
    return steps;
  }

  public FeedScopedId getId() {
    return id;
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
