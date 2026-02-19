package org.opentripplanner.street.model.edge;

import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

public class SimpleConcreteEdge extends Edge {

  /**
   * Constructor without ID.
   */
  private SimpleConcreteEdge(Vertex v1, Vertex v2) {
    super(v1, v2);
  }

  public static SimpleConcreteEdge createSimpleConcreteEdge(Vertex v1, Vertex v2) {
    return connectToGraph(new SimpleConcreteEdge(v1, v2));
  }

  @Override
  public State[] traverse(State s0) {
    double d = getDistanceMeters();
    TraverseMode mode = s0.currentMode();
    int t = (int) ((1000.0 * d) / getSpeed(mode, s0.getRequest(), false));
    StateEditor s1 = s0.edit(this);
    s1.incrementTimeInMilliseconds(t);
    s1.incrementWeight(d);
    return s1.makeStateArray();
  }

  @Override
  public I18NString getName() {
    return null;
  }

  @Override
  public double getDistanceMeters() {
    return SphericalDistanceLibrary.distance(
      getFromVertex().getCoordinate(),
      getToVertex().getCoordinate()
    );
  }

  /**
   * The road speed for a specific traverse mode.
   *
   */
  public static double getSpeed(TraverseMode mode, StreetSearchRequest req, boolean walkingBike) {
    return switch (mode) {
      case WALK -> walkingBike ? req.bike().walking().speed() : req.walk().speed();
      case BICYCLE -> req.bike().speed();
      case SCOOTER -> req.scooter().speed();
      default -> throw new IllegalArgumentException("getSpeed(): Invalid mode " + mode);
    };
  }
}
