package org.opentripplanner.ext.fares.model;

public sealed interface FareDistance {
  record Stops(int min, int max) implements FareDistance {}

  record LinearDistance(Distance min, Distance max) implements FareDistance {}
}
