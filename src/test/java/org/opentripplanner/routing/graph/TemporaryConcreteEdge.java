package org.opentripplanner.routing.graph;

import org.locationtech.jts.geom.LineString;
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

  public TemporaryConcreteEdge(TemporaryVertex v1, Vertex v2) {
    super((Vertex) v1, v2);
    if (v1.isEndVertex()) {
      throw new IllegalStateException("A temporary edge is directed away from an end vertex");
    }
  }

  public TemporaryConcreteEdge(Vertex v1, TemporaryVertex v2) {
    super(v1, (Vertex) v2);
    if (!v2.isEndVertex()) {
      throw new IllegalStateException("A temporary edge is directed towards a start vertex");
    }
  }

  @Override
  public State traverse(State s0) {
    double d = getDistanceMeters();
    TraverseMode mode = s0.getNonTransitMode();
    int t = (int) (d / s0.getPreferences().getSpeed(mode, false));
    StateEditor s1 = s0.edit(this);
    s1.incrementTimeInSeconds(t);
    s1.incrementWeight(d);
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
    return SphericalDistanceLibrary.distance(
      getFromVertex().getCoordinate(),
      getToVertex().getCoordinate()
    );
  }
}
