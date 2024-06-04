package org.opentripplanner.updater.trip;

public enum UpdateSemantics {
  /**
   * The update contains all available realtime information for a given feed. The previously stored
   * updates can be deleted and replaced with the information from this one.
   */
  FULL,
  /**
   * The update contains only information for a subset of all trips. If there is information stored
   * from a previous realtime update it must be kept in order to have a complete picture of all
   * trips.
   */
  INCREMENTAL,
}
