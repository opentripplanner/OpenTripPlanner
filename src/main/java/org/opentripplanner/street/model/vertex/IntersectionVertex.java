package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.graph.Graph;

/**
 * Represents an ordinary location in space, typically an intersection.
 */
public class IntersectionVertex extends StreetVertex {

  /**
   * Does this intersection have a traffic light?
   */
  public boolean trafficLight;

  /**
   * Is this a free-flowing intersection, i.e. should it have no delay at all? e.g., freeway ramps,
   * etc.
   */
  public boolean freeFlowing;

  //For testing only
  public IntersectionVertex(Graph g, String label, double x, double y, String name) {
    this(g, label, x, y, new NonLocalizedString(name));
  }

  public IntersectionVertex(Graph g, String label, double x, double y, I18NString name) {
    super(g, label, x, y, name);
    freeFlowing = false;
    trafficLight = false;
  }

  public IntersectionVertex(Graph g, String label, double x, double y) {
    this(g, label, x, y, new NonLocalizedString(label));
  }

  /** Returns true if this.freeFlowing or if it appears that this vertex is free-flowing */
  public boolean inferredFreeFlowing() {
    if (this.freeFlowing) {
      return true;
    }

    return getDegreeIn() == 1 && getDegreeOut() == 1 && !this.trafficLight;
  }
}
