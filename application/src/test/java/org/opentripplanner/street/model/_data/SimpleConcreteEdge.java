package org.opentripplanner.street.model._data;

import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
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
    RoutingPreferences preferences = s0.getPreferences();
    double t = d / preferences.getSpeed(mode, false);
    double reluctance =
      switch (mode) {
        case WALK -> preferences.walk().reluctance();
        case BICYCLE -> preferences.bike().reluctance();
        case SCOOTER -> preferences.scooter().reluctance();
        default -> 1;
      };
    double w = t * reluctance;
    StateEditor s1 = s0.edit(this);
    s1.incrementTimeInMilliseconds((int) (t * 1000));
    s1.incrementWeight(w);
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
}
