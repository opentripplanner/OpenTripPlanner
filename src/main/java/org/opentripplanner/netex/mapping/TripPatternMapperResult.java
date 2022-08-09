package org.opentripplanner.netex.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

/**
 * This mapper returnes two collections, so we need to use a simple wraper to be able to return the
 * result from the mapping method.
 */
class TripPatternMapperResult {

  /**
   * A map from trip/serviceJourney id to an ordered list of scheduled stop point ids.
   */
  final ArrayListMultimap<String, String> scheduledStopPointsIndex = ArrayListMultimap.create();

  final Map<Trip, List<StopTime>> tripStopTimes = new HashMap<>();

  final Multimap<StopPattern, TripPattern> tripPatterns = ArrayListMultimap.create();

  /**
   * stopTimes by the timetabled-passing-time id
   */
  final Map<String, StopTime> stopTimeByNetexId = new HashMap<>();

  final ArrayList<TripOnServiceDate> tripOnServiceDates = new ArrayList<>();
}
