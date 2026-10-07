package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;

/**
 * Encapsulate information about an access or egress path. This interface extends
 * {@link RaptorAccessEgress} with methods relevant only to street routing and access/egress
 * filtering.
 */
public interface RoutingAccessEgress extends RaptorAccessEgress {
  /**
   * Return a new copy of this with the requested penalty.
   * <p>
   * OVERRIDE THIS IF KEEPING THE TYPE IS IMPORTANT!
   */
  RoutingAccessEgress withPenalty(TimeAndCost penalty);

  /**
   * Return true if a vehicle picked up at a rental station is still rented at the end of the
   * street search, which is at the transit stop. For an egress, the search runs backward in time,
   * so this means the passenger arrives at the destination with the rented vehicle.
   */
  boolean isRentingVehicleFromStation();

  /**
   * Return true if any part of the access/egress is traversed by car.
   */
  boolean containsModeCar();

  /**
   * Return true if all edges are traversed on foot.
   */
  boolean isWalkOnly();

  TimeAndCost penalty();
}
