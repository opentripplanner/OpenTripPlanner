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
  /**
   * Link both ways
   */
  BOTH_WAYS;

  /**
   * Return {@code true} if either outgoing or both-ways.
   */
  public boolean allowOutgoing() {
    return this == OUTGOING || this == BOTH_WAYS;
  }

  /**
   * Return {@code true} if either incoming or both-ways.
   */
  public boolean allowIncoming() {
    return this == INCOMING || this == BOTH_WAYS;
  }
}
