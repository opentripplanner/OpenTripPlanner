package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.graph.Graph;

/**
 * A vertex representing a place along a street between two intersections that is not derived from
 * an OSM node, but is instead the result of breaking that street segment into two pieces in order
 * to connect it to a transit stop.
 */
public class SplitterVertex extends IntersectionVertex {

  private final String label;

  public SplitterVertex(Graph g, String label, double x, double y, I18NString name) {
    super(g, x, y, name, false, false);
    this.label = label;
  }

  public SplitterVertex(Graph g, String label, double x, double y, String name) {
    this(g, label, x, y, new NonLocalizedString(name));
  }

  public SplitterVertex(Graph g, String label, double x, double y) {
    this(g, label, x, y, new NonLocalizedString(label));
  }

  @Override
  public boolean inferredFreeFlowing() {
    // splitter vertices don't represent something that exists in the world, so traversing them is
    // always free.
    return true;
  }

  @Override
  public String getLabel() {
    return label;
  }
}
