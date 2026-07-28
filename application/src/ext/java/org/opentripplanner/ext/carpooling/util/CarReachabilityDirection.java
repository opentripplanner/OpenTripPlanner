package org.opentripplanner.ext.carpooling.util;

/**
 * The direction(s) of car travel a vertex must support. A point passed <em>through</em> must be
 * both reachable and leavable; a one-directional endpoint (e.g. the mouth of a one-way street) need
 * only support its own direction.
 *
 * @see CarReachableVertexSnapper
 */
public enum CarReachabilityDirection {
  /** A car must be able to drive <em>away</em> from the vertex. */
  DEPART(true, false),
  /** A car must be able to drive <em>to</em> the vertex. */
  ARRIVE(false, true),
  /** A car must be able to both reach and leave the vertex — a point it passes through. */
  THROUGH(true, true);

  private final boolean requiresDeparture;
  private final boolean requiresArrival;

  CarReachabilityDirection(boolean requiresDeparture, boolean requiresArrival) {
    this.requiresDeparture = requiresDeparture;
    this.requiresArrival = requiresArrival;
  }

  /** Whether a car must be able to drive away from the vertex. */
  public boolean requiresDeparture() {
    return requiresDeparture;
  }

  /** Whether a car must be able to drive to the vertex. */
  public boolean requiresArrival() {
    return requiresArrival;
  }
}
