package org.opentripplanner.ext.carpooling.routing;

import java.util.List;

/**
 * Associates a {@link CarpoolTripWithVertices} with the list of {@link ViableAccessEgress}
 * entries that passed heuristic filtering for that trip. Each viable access/egress
 * represents a transit stop that can potentially be served by this carpool trip.
 *
 * @param tripWithVertices the carpool trip with its resolved street graph vertices
 * @param viableAccessEgress the access/egress candidates that passed filtering for this trip
 */
public record TripWithViableAccessEgress(
  CarpoolTripWithVertices tripWithVertices,
  List<ViableAccessEgress> viableAccessEgress
) {}
