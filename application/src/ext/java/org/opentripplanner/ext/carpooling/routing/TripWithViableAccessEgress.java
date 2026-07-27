package org.opentripplanner.ext.carpooling.routing;

import java.util.List;

/**
 * A routed carpool trip paired with the access/egress candidates that passed filtering for it. Each
 * {@link ViableAccessEgress} represents a transit stop this trip can potentially serve.
 */
public record TripWithViableAccessEgress(
  RoutedCarpoolTrip routedTrip,
  List<ViableAccessEgress> viableAccessEgress
) {}
