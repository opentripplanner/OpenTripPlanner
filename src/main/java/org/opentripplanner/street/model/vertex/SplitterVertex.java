package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.graph.Graph;

/**
 * A vertex representing a place along a street between two intersections that is not derived from
 * an OSM node, but is instead the result of breaking that street segment into two pieces in order
 * to connect it to a transit stop.
 */
public class SplitterVertex extends IntersectionVertex {

  public SplitterVertex(Graph g, String label, double x, double y, I18NString name) {
    super(g, label, x, y, name);
    // splitter vertices don't represent something that exists in the world, so traversing them is
    // always free.
    this.freeFlowing = true;
  }
}
