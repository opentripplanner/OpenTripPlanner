package org.opentripplanner.routing.algorithm.raptoradapter.router.startonboardaccess;

public record LocationInTripPatternReference(
  int stopIndex,
  int stopPositionInPattern,
  int boardingTimeSeconds
) {}
