package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model.basic.I18NString;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.util.BitSetUtils;

/**
 * Represents an ordinary location in space, typically an intersection.
 */
public class IntersectionVertex extends StreetVertex {

  private static final long serialVersionUID = 1L;

  private static final int HIGHWAY_TRAFFIC_LIGHT_INDEX = 0;

  private static final int CROSSING_TRAFFIC_LIGHT_INDEX = 1;

  /**
   * SOME LOGIC IN THIS FILE IS BASED ON THAT THERE ARE ONLY THE CURRENT FLAGS AND IN THIS ORDER, IF
   * MORE FLAGS ARE ADDED, THE CURRENT LOGIC NEEDS TO AT LEAST BE REVIEWED AND MAYBE MODIFIED.
   */
  private short flags;

  //For testing only
  public IntersectionVertex(Graph g, String label, double x, double y, String name) {
    this(g, label, x, y, new NonLocalizedString(name));
  }

  public IntersectionVertex(Graph g, String label, double x, double y, I18NString name) {
    super(g, label, x, y, name);
  }

  public IntersectionVertex(Graph g, String label, double x, double y) {
    this(g, label, x, y, new NonLocalizedString(label));
  }

  /**
   * Does this intersection have a traffic light meant for cars (and for other means of traversing on such roads)?
   */
  public void setHighwayTrafficLight(boolean highwayTrafficLight) {
    flags = BitSetUtils.set(flags, HIGHWAY_TRAFFIC_LIGHT_INDEX, highwayTrafficLight);
  }

  /**
   * Does this intersection have a traffic light meant for pedestrians and cyclists?
   */
  public void setCrossingTrafficLight(boolean crossingTrafficLight) {
    flags = BitSetUtils.set(flags, CROSSING_TRAFFIC_LIGHT_INDEX, crossingTrafficLight);
  }

  /**
   * Takes into account both traffic lights meant for pedestrians and for cars as cyclists have
   * to obey both rules.
   */
  public boolean hasCyclingTrafficLight() {
    // return true if node has crossing or highway traffic light
    return !hasNoTrafficLight();
  }

  /**
   * Doesn't take into account traffic lights meant for cars.
   */
  public boolean hasWalkingTrafficLight() {
    return BitSetUtils.get(flags, CROSSING_TRAFFIC_LIGHT_INDEX);
  }

  /**
   * Doesn't take into account traffic lights meant for pedestrians.
   */
  public boolean hasDrivingTrafficLight() {
    return BitSetUtils.get(flags, HIGHWAY_TRAFFIC_LIGHT_INDEX);
  }

  /** Is this a free-flowing intersection, i.e. should it have no delay at all. */
  public boolean inferredFreeFlowing() {
    return getDegreeIn() == 1 && getDegreeOut() == 1 && hasNoTrafficLight();
  }

  /** Has no highway or crossing traffic light. */
  private boolean hasNoTrafficLight() {
    return flags == 0;
  }
}
