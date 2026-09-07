package org.opentripplanner.updater;

/**
 * Describes the incrementality of a collection of realtime updates and how they are related to previous
 * ones.
 */
public enum UpdateIncrementality {
  /**
   * The update contains all available realtime information for a given feed. The previously stored
   * updates must be deleted and replaced with the information from this one.
   */
  FULL_DATASET,
  /**
   * The update contains only information for a subset of all entities. If there is information
   * stored from a previous realtime update it must be kept in order to have a complete picture.
   */
  DIFFERENTIAL,
}
