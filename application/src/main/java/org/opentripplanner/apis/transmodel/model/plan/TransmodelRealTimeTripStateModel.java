package org.opentripplanner.apis.transmodel.model.plan;

/**
 * API model representing the real-time state of a trip on a leg, used as the source object for
 * the {@code RealTimeTripState} GraphQL type in the Transmodel API.
 */
public record TransmodelRealTimeTripStateModel(
  boolean added,
  boolean canceled,
  boolean deleted,
  boolean timesModified,
  boolean tripPatternModified,
  boolean updated
) {}
