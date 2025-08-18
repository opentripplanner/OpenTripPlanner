package org.opentripplanner.street.model.edge;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

/**
 * An edge that has a trivial cost to traverse.
 *
 * @author novalis
 */
public class FreeEdge extends Edge {

  protected FreeEdge(Vertex from, Vertex to) {
    super(from, to);
  }

  public static FreeEdge createFreeEdge(Vertex from, Vertex to) {
    return connectToGraph(new FreeEdge(from, to));
  }

  @Override
  public State[] traverse(State s0) {
    StateEditor s1 = s0.edit(this);
    // This edge is used to connect vertices not on the street network, such as origin, destination
    // and station centroids, onto the street network. By adding a small cost, we prevent a normal
    // street search from going via these vertices.
    s1.incrementWeight(1);
    s1.setBackMode(null);
    return s1.makeStateArray();
  }

  @Override
  public I18NString getName() {
    return null;
  }
}
