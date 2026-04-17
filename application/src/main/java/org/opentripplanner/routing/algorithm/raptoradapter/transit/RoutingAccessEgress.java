package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.util.List;
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
   * Return the final states of the A* street search that reached the transit stop. "Final" refers
   * to the search order, not chronological order — for egress searches
   * ({@code request.arriveBy() == true}) the state chain runs backward in time and is not reversed.
   * Callers that need a chronological state chain must wrap this in a
   * {@link org.opentripplanner.astar.model.GraphPath}. For access, this is a list of final states
   * starting from origin to the access stop split at via locations visited inside the access. For
   * egress, this is a list starting at the egress stop ending at the destination split at the via
   * locations visited inside the egress.
   */
  List<State> getFinalStates();

  /**
   * Return true if all edges are traversed on foot.
   */
  boolean isWalkOnly();

  TimeAndCost penalty();
}
