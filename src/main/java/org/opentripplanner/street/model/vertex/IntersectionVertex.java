package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.lang.BitSetUtils;

/**
 * Represents an ordinary location in space, typically an intersection.
 */
public abstract class IntersectionVertex extends StreetVertex {

  private static final int HIGHWAY_TRAFFIC_LIGHT_INDEX = 0;

  private static final int CROSSING_TRAFFIC_LIGHT_INDEX = 1;

  /**
   * SOME LOGIC IN THIS FILE IS BASED ON THAT THERE ARE ONLY THE CURRENT FLAGS AND IN THIS ORDER, IF
   * MORE FLAGS ARE ADDED, THE CURRENT LOGIC NEEDS TO AT LEAST BE REVIEWED AND MAYBE MODIFIED.
   */
  private final short flags;

  protected IntersectionVertex(
    double x,
    double y,
    boolean hasHighwayTrafficLight,
    boolean hasCrossingTrafficLight
  ) {
    super(x, y);
    flags = initFlags(hasHighwayTrafficLight, hasCrossingTrafficLight);
  }

  protected IntersectionVertex(double x, double y) {
    this(x, y, false, false);
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

  private static short initFlags(boolean highwayTrafficLight, boolean crossingTrafficLight) {
    short flags = 0;
    flags = BitSetUtils.set(flags, HIGHWAY_TRAFFIC_LIGHT_INDEX, highwayTrafficLight);
    flags = BitSetUtils.set(flags, CROSSING_TRAFFIC_LIGHT_INDEX, crossingTrafficLight);
    return flags;
  }
}
