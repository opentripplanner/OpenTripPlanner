package org.opentripplanner.ext.flex.flexpathcalculator;

/**
 * This class contains the results from a FlexPathCalculator.
 */
public class FlexPath {

  public int distanceMeters;
  public int durationSeconds;

  public FlexPath(int distanceMeters, int durationSeconds) {
    this.distanceMeters = distanceMeters;
    this.durationSeconds = durationSeconds;
  }
}
