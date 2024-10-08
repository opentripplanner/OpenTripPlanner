package org.opentripplanner.openstreetmap.wayproperty;

/**
 * Record that holds forward and back safety factors for cycling or walking.
 */
public record SafetyFeatures(double forward, double back) {
  public static final SafetyFeatures DEFAULT = new SafetyFeatures(1, 1);

  /**
   * Does this instance actually modify the safety values?
   */
  public boolean modifies() {
    return !(forward == 1 && back == 1);
  }

  /**
   * Does forward and back have the same value?
   */
  public boolean isSymmetric() {
    return forward == back;
  }
}
