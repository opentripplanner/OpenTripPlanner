package org.opentripplanner.updater.trip.siri;

import com.google.common.collect.ImmutableSetMultimap;
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

  private final ImmutableSetMultimap<String, Trip> internalPlanningCodeCache;
  private final ImmutableSetMultimap<String, Trip> startStopTripCache;

  public SiriFuzzyTripMatcherCache(TransitRepository transitRepository) {
    TransitService index = new DefaultTransitService(transitRepository, null);
    var internalPlanningCodes = ImmutableSetMultimap.<String, Trip>builder();
    var startStopTrips = ImmutableSetMultimap.<String, Trip>builder();

    for (Trip trip : index.listTrips()) {
      TripPattern tripPattern = index.findPattern(trip);

      if (tripPattern == null) {
        continue;
      }

      if (tripPattern.getRoute().getMode().equals(TransitMode.RAIL)) {
        String internalPlanningCode = trip.getNetexInternalPlanningCode();
        if (internalPlanningCode != null) {
          internalPlanningCodes.put(internalPlanningCode, trip);
        }
      }
      String lastStopId = tripPattern.lastStop().getId().getId();

      TripTimes tripTimes = tripPattern.getScheduledTimetable().getTripTimes(trip);
      if (tripTimes != null) {
        int arrivalTime = tripTimes.getArrivalTime(tripTimes.getNumStops() - 1);
        startStopTrips.put(startStopKey(lastStopId, arrivalTime), trip);
      }
    }

    this.internalPlanningCodeCache = internalPlanningCodes.build();
    this.startStopTripCache = startStopTrips.build();

    LOG.info("Built internalPlanningCode-cache [{}].", internalPlanningCodeCache.keySet().size());
    LOG.info("Built start-stop-cache [{}].", startStopTripCache.keySet().size());
  }

  /**
   * The rail trips with the given NeTEx internal planning code, or an empty set if there are none.
   */
  public Set<Trip> tripsByInternalPlanningCode(String internalPlanningCode) {
    return internalPlanningCodeCache.get(internalPlanningCode);
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
