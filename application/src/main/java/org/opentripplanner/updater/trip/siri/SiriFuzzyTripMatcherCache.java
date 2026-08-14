package org.opentripplanner.updater.trip.siri;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduled-data cache for {@link SiriFuzzyTripMatcher}. Built once from the static transit model
 * and shared across all SIRI updaters. The maps are immutable after construction.
 */
public class SiriFuzzyTripMatcherCache {

  private static final Logger LOG = LoggerFactory.getLogger(SiriFuzzyTripMatcherCache.class);

  final Map<String, Set<Trip>> internalPlanningCodeCache = new HashMap<>();
  final Map<String, Set<Trip>> startStopTripCache = new HashMap<>();

  public SiriFuzzyTripMatcherCache(TransitRepository transitRepository) {
    initCache(new DefaultTransitService(transitRepository, null));
  }

  private void initCache(TransitService index) {
    for (Trip trip : index.listTrips()) {
      TripPattern tripPattern = index.findPattern(trip);

      if (tripPattern == null) {
        continue;
      }

      if (tripPattern.getRoute().getMode().equals(TransitMode.RAIL)) {
        String internalPlanningCode = trip.getNetexInternalPlanningCode();
        if (internalPlanningCode != null) {
          internalPlanningCodeCache
            .computeIfAbsent(internalPlanningCode, key -> new HashSet<>())
            .add(trip);
        }
      }
      String lastStopId = tripPattern.lastStop().getId().getId();

      TripTimes tripTimes = tripPattern.getScheduledTimetable().getTripTimes(trip);
      if (tripTimes != null) {
        int arrivalTime = tripTimes.getArrivalTime(tripTimes.getNumStops() - 1);
        String key = lastStopId + ":" + arrivalTime;
        startStopTripCache.computeIfAbsent(key, k -> new HashSet<>()).add(trip);
      }
    }

    LOG.info("Built internalPlanningCode-cache [{}].", internalPlanningCodeCache.size());
    LOG.info("Built start-stop-cache [{}].", startStopTripCache.size());
  }
}
