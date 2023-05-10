package org.opentripplanner.model.fare;

import org.opentripplanner.framework.lang.Sandbox;

/** Represents a distance metric used in distance-based fare computation*/
@Sandbox
public sealed interface FareDistance {
  /** Represents the number of stops as a distance metric in fare computation */
  record Stops(int min, int max) implements FareDistance {}

  /**
   * Represents the linear distance between the origin and destination points as a distance metric
   * in fare computation
   */
  record LinearDistance(Distance min, Distance max) implements FareDistance {}
}
