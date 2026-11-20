package org.opentripplanner.updater.trip.siri;

import com.google.common.collect.ImmutableSetMultimap;
import java.util.Set;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduled-data cache for {@link SiriFuzzyTripMatcher}. Built once from the static transit model
 * and shared across all SIRI updaters. The maps are immutable after construction.
 */
public class SiriFuzzyTripMatcherCache {

  private static final Logger LOG = LoggerFactory.getLogger(SiriFuzzyTripMatcherCache.class);

  private final ImmutableSetMultimap<String, Trip> startStopTripCache;

  private SiriFuzzyTripMatcherCache(ImmutableSetMultimap<String, Trip> startStopTripCache) {
    this.startStopTripCache = startStopTripCache;
  }

  /**
   * @param index a transit service without real-time data, the cache is built from the scheduled
   *              trips only.
   */
  public static SiriFuzzyTripMatcherCache create(TransitService index) {
    var startStopTrips = ImmutableSetMultimap.<String, Trip>builder();

    for (Trip trip : index.listTrips()) {
      TripPattern tripPattern = index.findPattern(trip);

      if (tripPattern == null) {
        continue;
      }

      String lastStopId = tripPattern.lastStop().getId().getId();

      var tripTimes = tripPattern.getScheduledTimetable().getTripTimes(trip);
      if (tripTimes != null) {
        int arrivalTime = tripTimes.getArrivalTime(tripTimes.getNumStops() - 1);
        startStopTrips.put(startStopKey(lastStopId, arrivalTime), trip);
      }
    }

    var cache = new SiriFuzzyTripMatcherCache(startStopTrips.build());

    LOG.info("Built start-stop-cache [{}].", cache.startStopTripCache.keySet().size());
    return cache;
  }

  /**
   * The trips whose scheduled last stop and arrival time (in seconds since start of the service
   * day) match the given values, or an empty set if there are none.
   */
  public Set<Trip> tripsByLastStopArrival(String stopId, int arrivalTime) {
    return startStopTripCache.get(startStopKey(stopId, arrivalTime));
  }

  private static String startStopKey(String stopId, int arrivalTime) {
    return stopId + ":" + arrivalTime;
  }
}
