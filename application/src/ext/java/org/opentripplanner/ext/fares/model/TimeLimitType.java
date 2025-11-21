package org.opentripplanner.ext.fares.model;

/**
 * How fare validity durations should be computed across a set of legs for fare calculation purposes.
 */
public enum TimeLimitType {
  /**
   * The duration is to be computed from the departure time of the current leg to the arrival time of
   * the next one.
   */
  DEPARTURE_TO_ARRIVAL,
  /**
   * The duration is to be computed from the departure time of the current leg to the departure time of
   * the next one.
   */
  DEPARTURE_TO_DEPARTURE,
  /**
   * The duration is to be computed from the arrival time of the current leg to the departure time of
   * the next one.
   */
  ARRIVAL_TO_DEPARTURE,
  /**
   * The duration is to be computed from the arrival time of the current leg to the arrival time of
   * the next one.
   */
  ARRIVAL_TO_ARRIVAL,
}
