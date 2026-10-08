package org.opentripplanner.street.model.path;

import java.util.Collection;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/// This class keeps track of which graph vertices have been visited and the paths there.
public class StreetPathTree {

  private final ShortestPathTree<State, Edge, Vertex> spt;

  public StreetPathTree(ShortestPathTree<State, Edge, Vertex> spt) {
    this.spt = spt;
  }

  /// Returns the 'best' path for the given Vertex, where 'best' depends on the implementation.
  @Nullable
  public LazyStreetPath getPath(Vertex dest) {
    var state = spt.getState(dest);
    if (state == null) {
      return null;
    }
    return new LazyStreetPath(state);
  }

  /// Return every state in this tree.
  public Collection<State> getAllStates() {
    return spt.getAllStates();
  }
}
