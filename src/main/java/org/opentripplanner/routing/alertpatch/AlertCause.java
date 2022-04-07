package org.opentripplanner.routing.alertpatch;

/**
 * An enum containing cause options for an alert.
 */
public enum AlertCause {
  /**
   * Unknown cause.
   */
  UNKNOWN_CAUSE,
  /**
   * Other cause than the available options.
   */
  OTHER_CAUSE,
  /**
   * Technical problem.
   */
  TECHNICAL_PROBLEM,
  /**
   * Strike.
   */
  STRIKE,
  /**
   * Demonstration.
   */
  DEMONSTRATION,
  /**
   * Accident.
   */
  ACCIDENT,
  /**
   * Holiday.
   */
  HOLIDAY,
  /**
   * Weather.
   */
  WEATHER,
  /**
   * Maintenance.
   */
  MAINTENANCE,
  /**
   * Construction.
   */
  CONSTRUCTION,
  /**
   * Police activity.
   */
  POLICE_ACTIVITY,
  /**
   * Medical emergency.
   */
  MEDICAL_EMERGENCY,
}
