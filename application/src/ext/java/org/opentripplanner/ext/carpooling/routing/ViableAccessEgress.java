package org.opentripplanner.ext.carpooling.routing;

import java.util.List;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * A transit stop that has been determined viable for carpooling access or egress,
 * along with the pre-computed insertion positions where the passenger can be
 * picked up and dropped off on the carpool trip's route.
 *
 * @param transitStop the nearby transit stop
 * @param transitVertex the street graph vertex for the transit stop
 * @param passengerVertex the street graph vertex for the passenger's origin if access, and the passenger's destination if egress
 * @param accessEgress whether this represents access (origin to transit) or egress (transit to destination)
 * @param insertionPositions the viable pickup/dropoff positions on the carpool route
 */
public record ViableAccessEgress(
  NearbyStop transitStop,
  Vertex transitVertex,
  Vertex passengerVertex,
  AccessEgressType accessEgress,
  List<InsertionPosition> insertionPositions
) {}
