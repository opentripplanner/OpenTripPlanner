package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.I18NString;

/**
 * A vertex representing a place along a street between two intersections that is not derived from
 * an OSM node, but is instead the result of breaking that street segment into two pieces in order
 * to connect it to a transit stop.
 */
public class SplitterVertex extends IntersectionVertex {

  private final VertexLabel label;

  public SplitterVertex(String label, double x, double y, I18NString name) {
    super(x, y, name, false, false);
    this.label = VertexLabel.string(label);
  }

  @Override
  public VertexLabel getLabel() {
    return label;
  }

  @Override
  public boolean inferredFreeFlowing() {
    // splitter vertices don't represent something that exists in the world, so traversing them is
    // always free.
    return true;
  }
}
