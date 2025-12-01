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
   * For access, this is a list of states starting from origin to the access stop split at via
   * locations visited inside the access. For egress, this is a list starting at the egress stop
   * ending at the destination split at the via locations visited inside the egress.
   */
  List<State> getLastStates();

  /**
   * Return true if all edges are traversed on foot.
   */
  boolean isWalkOnly();

  TimeAndCost penalty();
}
