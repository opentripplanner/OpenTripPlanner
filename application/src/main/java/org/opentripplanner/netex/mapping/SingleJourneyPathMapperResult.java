package org.opentripplanner.netex.mapping;

import java.util.List;
import java.util.Map;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * The trip pattern of a SingleJourneyPath, with the trips of its SingleJourneys.
 */
record SingleJourneyPathMapperResult(
  TripPattern tripPattern,
  Map<Trip, List<StopTime>> stopTimesByTrip
) {}
