package org.opentripplanner.street.model.edge;

import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

public class SimpleConcreteEdge extends Edge {

  public SimpleConcreteEdge(Vertex v1, Vertex v2) {
    super(v1, v2);
  }

  @Override
  public I18NString getName() {
    return null;
  }

  @Override
  public State[] traverse(State s0) {
    return State.empty();
  }
}
