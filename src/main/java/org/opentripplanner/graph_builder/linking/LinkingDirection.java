package org.opentripplanner.graph_builder.linking;

public enum LinkingDirection {
  /**
   * From the new vertex towards the main graph
   */
  INCOMING,
  /**
   * From the main graph towards the new vertex
   */
  OUTGOING,
  BOTH_WAYS
}
