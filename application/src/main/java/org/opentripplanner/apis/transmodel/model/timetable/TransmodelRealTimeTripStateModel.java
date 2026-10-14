package org.opentripplanner.apis.transmodel.model.timetable;

import org.opentripplanner.transit.model.timetable.RealTimeTripState;

/**
 * API model representing the real-time state of a service journey, used as the source object for
 * the {@code RealTimeJourneyState} GraphQL type in the Transmodel API.
 */
public record TransmodelRealTimeTripStateModel(
  boolean extraJourney,
  boolean cancellation,
  boolean timesModified,
  boolean journeyPatternModified,
  boolean updated
) {
  public static TransmodelRealTimeTripStateModel of(RealTimeTripState state) {
    return new TransmodelRealTimeTripStateModel(
      state.added(),
      state.canceled(),
      state.timesModified(),
      state.tripPatternModified(),
      state.hasAnyUpdates()
    );
  }
}
