package org.opentripplanner.openstreetmap.wayproperty;

/**
 * Record that holds forward and back safety factors for cycling or walking.
 */
public record SafetyFeatures(double forward, double back) {
  public static final SafetyFeatures DEFAULT = new SafetyFeatures(1, 1);

  public boolean modifies() {
    return !(forward == 1 && back == 1);
  }

  public boolean isSymetric() {
    return forward == back;
  }
}
