package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.street.search.state.State;

/**
 * Encapsulate information about an access or egress path. This interface extends
 * {@link RaptorAccessEgress} with methods relevant only to street routing and
 * access/egress filtering.
 */
public interface RoutingAccessEgress extends RaptorAccessEgress {
  /**
   * Return a new copy of this with the requested penalty.
   * <p>
   * OVERRIDE THIS IF KEEPING THE TYPE IS IMPORTANT!
   */
  RoutingAccessEgress withPenalty(TimeAndCost penalty);

  /**
   * Return the terminal state of the A* street search that reached the transit stop. "Last"
   * refers to the search order, not chronological order — for egress searches
   * ({@code request.arriveBy() == true}) the state chain runs backward in time and is not
   * reversed. Callers that need a chronological state chain must wrap this in a
   * {@link org.opentripplanner.astar.model.GraphPath}.
   */
  State getLastState();

  /**
   * Return true if all edges are traversed on foot.
   */
  boolean isWalkOnly();

  TimeAndCost penalty();
}
