package org.opentripplanner.routing.linking;

public enum Scope {
  /**
   * Permanent changes to the street graph done during graph building.
   */
  PERMANENT,
  /**
   * Temporary changes done to the street graph by updaters. These should be visible to routing
   * requests, but not to each other.
   */
  REALTIME,
  /**
   * Temporary changes made by a single routing request. These should only be visible to the same
   * routing request.
   */
  REQUEST,
}
