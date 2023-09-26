package org.opentripplanner.astar.spi;

import javax.annotation.Nonnull;

/**
 * Represents an edge in the street network. Most edges have a one-to-one mapping to real world
 * things like street segments or stairs.
 * However, there can be other edges that represent concepts that are invisible and only present
 * in OTP's model. These are things that link entities like transit stops and rental vehicles to the
 * street network. The links are necessary in order for A* to discover them and can contain logic for
 * access permissions. For example, a car can not use an edge that links the
 * street network with a transit stop to prevent a straight transfer from car to transit.
 */
public interface AStarEdge<
  State extends AStarState<State, Edge, Vertex>,
  Edge extends AStarEdge<State, Edge, Vertex>,
  Vertex extends AStarVertex<State, Edge, Vertex>
> {
  Vertex getFromVertex();

  Vertex getToVertex();

  /**
   * Traverse the edge from a given state and return the result of the traversal.
   *
   * @param s0 The 'current' state when arriving at the fromVertex.
   * @return The array of states that are the result of the state (the passenger) moving (traversing)
   *         through the edge.
   *         <p>
   *         In most cases this is a single state where, for example, the weight (cost) and time are
   *         increased according to the properties of the edge.
   *         <p>
   *         However, it is also possible that this edge is not traversable for the current state,
   *         for example if it's a walk-only edge but the state is arriving in a car. In such a
   *         case an empty array is returned (see {@link org.opentripplanner.street.search.state.State#empty()}).
   *         The Astar algorithm won't then explore this state any further as the destination is not
   *         reachable through this state/edge combination.
   *         <p>
   *         Lastly, there can also be cases where more than one state is returned: For example
   *         when a state is renting a free-floating vehicle but the edge is in a no-drop-off zone.
   *         In such a case two resulting states are possible: one where the state continues on the
   *         rental vehicle (as you may exit the no-drop-off zone later) and a second state where the
   *         vehicle is speculatively dropped off and the passenger continues on foot in case
   *         that the destination is inside the zone.
   */
  @Nonnull
  State[] traverse(State s0);
}
