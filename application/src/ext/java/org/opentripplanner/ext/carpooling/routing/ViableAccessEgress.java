package org.opentripplanner.ext.carpooling.routing;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * A transit stop that has been determined viable for carpooling access or egress,
 * along with the pre-computed insertion positions where the passenger can be
 * picked up and dropped off on the carpool trip's route.
 *
 * @param transitStop the nearby transit stop
 * @param transitVertex the street graph vertex where the driver stops to pick up or drop off the
 *        passenger at the transit-stop side (already snapped to a car-accessible vertex)
 * @param passengerVertex the street graph vertex where the driver stops at the passenger side
 *        — origin for access, destination for egress — already snapped to a car-accessible vertex
 * @param accessEgress whether this represents access (origin to transit) or egress (transit to destination)
 * @param insertionPositions the viable pickup/dropoff positions on the carpool route
 * @param walkToPickup walk path from the passenger-side (or transit-stop-side) location to the
 *        snapped pickup vertex. Null if no walking is needed at the pickup end.
 * @param walkFromDropoff walk path from the snapped dropoff vertex to the final location. Null if
 *        no walking is needed at the dropoff end.
 */
public record ViableAccessEgress(
  NearbyStop transitStop,
  Vertex transitVertex,
  Vertex passengerVertex,
  AccessEgressType accessEgress,
  List<InsertionPosition> insertionPositions,
  @Nullable GraphPath<State, Edge, Vertex> walkToPickup,
  @Nullable GraphPath<State, Edge, Vertex> walkFromDropoff
) {}
