package org.opentripplanner.ext.flex.distancecalculator;

/**
 * This class contains the results from a DistanceCalculator.
 */
public class DistanceAndDuration {

  public int distanceMeters;
  public int durationSeconds;

  public DistanceAndDuration(int distanceMeters, int durationSeconds) {
    this.distanceMeters = distanceMeters;
    this.durationSeconds = durationSeconds;
  }
}
