package org.opentripplanner.street.model.edge;

import javax.annotation.Nonnull;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

/**
 * An edge that costs nothing to traverse. Used for connecting intersection vertices to the main
 * edge-based graph.
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

  public String toString() {
    return "FreeEdge(" + fromv + " -> " + tov + ")";
  }

  @Override
  @Nonnull
  public State[] traverse(State s0) {
    StateEditor s1 = s0.edit(this);
    s1.incrementWeight(1);
    s1.setBackMode(null);
    return s1.makeStateArray();
  }

  @Override
  public I18NString getName() {
    return null;
  }
}
