package org.opentripplanner.apis.transmodel.model.plan;

/**
 * API model representing the real-time state of a service journey, used as the source object for
 * the {@code RealTimeJourneyState} GraphQL type in the Transmodel API.
 */
public record TransmodelRealTimeTripStateModel(
  boolean extraJourney,
  boolean cancellation,
  boolean deleted,
  boolean timesModified,
  boolean journeyPatternModified,
  boolean updated
) {}
