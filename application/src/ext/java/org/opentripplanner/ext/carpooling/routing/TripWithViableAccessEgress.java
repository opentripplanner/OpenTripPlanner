package org.opentripplanner.ext.carpooling.routing;

import java.util.List;

/**
 * Associates a {@link RoutableCarpoolTrip} with the list of {@link ViableAccessEgress} entries for
 * that trip: the corridor stops that serve the passenger's role, with a walk within the passenger's
 * walk budget. Each viable access/egress represents a transit stop that can potentially be served
 * by this carpool trip.
 *
 * @param routableTrip       the carpool trip
 * @param viableAccessEgress the access/egress candidates for this trip
 */
public record TripWithViableAccessEgress(
  RoutableCarpoolTrip routableTrip,
  List<ViableAccessEgress> viableAccessEgress
) {}
