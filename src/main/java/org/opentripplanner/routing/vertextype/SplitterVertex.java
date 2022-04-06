package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;

/**
 * A vertex representing a place along a street between two intersections that is not derived from an OSM node,
 * but is instead the result of breaking that street segment into two pieces in order to connect it to
 * a transit stop.
 */
public class SplitterVertex extends IntersectionVertex {

  public SplitterVertex(Graph g, String label, double x, double y) {
    super(g, label, x, y);
    // splitter vertices don't represent something that exists in the world, so traversing them is
    // always free.
    this.freeFlowing = true;
  }

  private static final long serialVersionUID = 1L;
}
