package org.opentripplanner.street.model.edge;

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
  /**
   * Link both ways
   */
  BIDIRECTIONAL;

  /**
   * Return {@code true} if either outgoing or both-ways.
   */
  public boolean allowOutgoing() {
    return this == OUTGOING || this == BIDIRECTIONAL;
  }

  /**
   * Return {@code true} if either incoming or both-ways.
   */
  public boolean allowIncoming() {
    return this == INCOMING || this == BIDIRECTIONAL;
  }
}
