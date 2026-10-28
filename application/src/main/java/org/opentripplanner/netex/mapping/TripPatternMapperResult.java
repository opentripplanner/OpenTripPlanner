package org.opentripplanner.netex.mapping;

import com.google.common.collect.ArrayListMultimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

/**
 * Wrapper class for the result of TripPatternMapper
 *
 * @param tripPatternGeometry      the hop geometries and cumulative distances of the trip pattern
 * @param scheduledStopPointsIndex A map from trip/serviceJourney id to an ordered list of scheduled
 *                                 stop point ids.
 * @param stopTimeByNetexId        stopTimes by the timetabled-passing-time id
 */
record TripPatternMapperResult(
  TripPattern tripPattern,
  CompactLineStringSequence tripPatternGeometry,
  ArrayListMultimap<String, String> scheduledStopPointsIndex,
  Map<Trip, List<StopTime>> tripStopTimes,
  Map<String, StopTime> stopTimeByNetexId,
  ArrayList<TripOnServiceDate> tripOnServiceDates
) {}
