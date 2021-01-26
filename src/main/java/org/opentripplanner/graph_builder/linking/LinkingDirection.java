package org.opentripplanner.graph_builder.linking;

public enum LinkingDirection {
  /**
   * From the new vertex towards the main graph
   */
  FORWARD,
  /**
   * From the main graph towards the new vertex
   */
  BACKWARD,
  BOTH_WAYS
}
