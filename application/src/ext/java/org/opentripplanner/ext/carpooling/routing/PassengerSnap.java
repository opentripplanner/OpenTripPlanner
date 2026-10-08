package org.opentripplanner.ext.carpooling.routing;

import javax.annotation.Nullable;
import org.opentripplanner.ext.carpooling.model.GraphPath;
import org.opentripplanner.street.model.vertex.Vertex;

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
  @Nullable GraphPath walkToPickup,
  @Nullable GraphPath walkFromDropoff
) {}
