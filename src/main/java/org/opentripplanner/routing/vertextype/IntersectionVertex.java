package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.basic.I18NString;
import org.opentripplanner.transit.model.basic.NonLocalizedString;

/**
 * Represents an ordinary location in space, typically an intersection.
 */
public class IntersectionVertex extends StreetVertex {

  private static final long serialVersionUID = 1L;

  private boolean highwayTrafficLight;

  private boolean crossingTrafficLight;

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
    highwayTrafficLight = false;
    crossingTrafficLight = false;
  }

  public IntersectionVertex(Graph g, String label, double x, double y) {
    this(g, label, x, y, new NonLocalizedString(label));
  }

  /**
   * Does this intersection have a traffic light meant for cars (and for other means of traversing on such roads)?
   */
  public void setHighwayTrafficLight(boolean highwayTrafficLight) {
    this.highwayTrafficLight = highwayTrafficLight;
  }

  /**
   * Does this intersection have a traffic light meant for pedestrians and cyclists?
   */
  public void setCrossingTrafficLight(boolean crossingTrafficLight) {
    this.crossingTrafficLight = crossingTrafficLight;
  }

  /**
   * Takes into account both traffic lights meant for pedestrians and for cars as cyclists have
   * to obey both rules.
   */
  public boolean hasCyclingTrafficLight() {
    return this.highwayTrafficLight || this.crossingTrafficLight;
  }

  /**
   * Doesn't take into account traffic lights meant for cars.
   */
  public boolean hasWalkingTrafficLight() {
    return this.crossingTrafficLight;
  }

  /**
   * Doesn't take into account traffic lights meant for pedestrians.
   */
  public boolean hasDrivingTrafficLight() {
    return this.highwayTrafficLight;
  }

  /** Returns true if this.freeFlowing or if it appears that this vertex is free-flowing */
  public boolean inferredFreeFlowing() {
    if (this.freeFlowing) {
      return true;
    }

    return (
      getDegreeIn() == 1 && getDegreeOut() == 1 && !highwayTrafficLight && !crossingTrafficLight
    );
  }
}
