package org.opentripplanner.routing.linking;

/**
 * Represents the direction of travel of the edges created when linking a vertex into the street
 * graph
 */
public enum LinkingDirection {
  /**
   * From the new vertex towards the main graph
   */
  INCOMING,
  /**
   * From the main graph towards the new vertex
   */
  OUTGOING,
  BOTH_WAYS,
}
