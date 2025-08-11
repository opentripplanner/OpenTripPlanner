package org.opentripplanner.ext.carpooling.model;

import org.opentripplanner.routing.graphfinder.NearbyStop;

public record CarpoolItineraryCandidate(
  CarpoolTrip trip,
  NearbyStop boardingStop,
  NearbyStop alightingStop
) {}