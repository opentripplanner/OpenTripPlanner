package org.opentripplanner.apis.gtfs.model;

/**
 * API model representing the real-time state of a trip, used as the source object for the
 * {@code RealTimeTripState} GraphQL type.
 */
public record RealTimeTripStateModel(
  boolean added,
  boolean canceled,
  boolean timesModified,
  boolean tripPatternModified,
  boolean updated
) {}
