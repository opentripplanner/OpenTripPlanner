package org.opentripplanner.routing.graph;

import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.TemporaryEdge;
import org.opentripplanner.street.model.vertex.TemporaryVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

public class TemporaryConcreteEdge extends Edge implements TemporaryEdge {

  private TemporaryConcreteEdge(TemporaryVertex v1, Vertex v2) {
    super((Vertex) v1, v2);
  }

  private TemporaryConcreteEdge(Vertex v1, TemporaryVertex v2) {
    super(v1, (Vertex) v2);
  }

  public static TemporaryConcreteEdge createTemporaryConcreteEdge(TemporaryVertex v1, Vertex v2) {
    return connectToGraph(new TemporaryConcreteEdge(v1, v2));
  }

  public static TemporaryConcreteEdge createTemporaryConcreteEdge(Vertex v1, TemporaryVertex v2) {
    return connectToGraph(new TemporaryConcreteEdge(v1, v2));
  }

  @Override
  public State[] traverse(State s0) {
    double d = getDistanceMeters();
    TraverseMode mode = s0.currentMode();
    int t = (int) (1000.0 * d / s0.getPreferences().getSpeed(mode, false));
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
}
