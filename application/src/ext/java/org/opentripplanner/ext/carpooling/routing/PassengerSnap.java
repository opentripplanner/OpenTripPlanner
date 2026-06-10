package org.opentripplanner.ext.carpooling.routing;

import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * Both ends of a passenger's carpool leg, already snapped to vertices the driver can stop at.
 *
 * @param pickupVertex   where the driver picks up
 * @param dropoffVertex  where the driver drops off
 * @param walkToPickup   walk from the passenger-side origin to {@code pickupVertex}, or
 *                       {@code null} if the passenger boards at the snapped vertex itself
 * @param walkFromDropoff walk from {@code dropoffVertex} to the passenger-side destination, or
 *                        {@code null} if the passenger alights at the snapped vertex itself
 */
public record PassengerSnap(
  Vertex pickupVertex,
  Vertex dropoffVertex,
  @Nullable GraphPath<State, Edge, Vertex> walkToPickup,
  @Nullable GraphPath<State, Edge, Vertex> walkFromDropoff
) {}
