package org.opentripplanner.raptor.api.view;

/**
 *
 * @param routeIndex            The index of the boarded route
 * @param tripScheduleIndex     The index of the boarded trip within the route
 * @param stopPositionInPattern The position in the route pattern of the first stop in the journey,
 *                              where the access path just arrived at. Since this is an on-board
 *                              access, this stop represents the most recently visited stop on the
 *                              currently boarded trip. The next stop position after this is the
 *                              first you can alight.
 */
public record TripScheduleStopPosition(
  int routeIndex,
  int tripScheduleIndex,
  int stopPositionInPattern
) {}
