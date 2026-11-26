package org.opentripplanner.updater.trip.siri;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduled-data cache for {@link SiriFuzzyTripMatcher}. Built once from the static transit model
 * and shared across all SIRI updaters. The maps are not modified after construction.
 * <p>
 * The scheduled arrival times at the last stop are indexed per stop in a sorted map, so the cache
 * can answer lookups within a time window. This allows each updater to use its own time tolerance
 * with the same shared cache.
 */
public class SiriFuzzyTripMatcherCache {

  private static final Logger LOG = LoggerFactory.getLogger(SiriFuzzyTripMatcherCache.class);

  private final ImmutableSetMultimap<String, Trip> internalPlanningCodeCache;

  /**
   * Trips by the id of their scheduled last stop and the scheduled arrival time at that stop (in
   * seconds since start of the service day).
   */
  private final Map<String, NavigableMap<Integer, Set<Trip>>> lastStopArrivalCache;

  private SiriFuzzyTripMatcherCache(
    ImmutableSetMultimap<String, Trip> internalPlanningCodeCache,
    Map<String, NavigableMap<Integer, Set<Trip>>> lastStopArrivalCache
  ) {
    this.internalPlanningCodeCache = internalPlanningCodeCache;
    this.lastStopArrivalCache = lastStopArrivalCache;
  }

  /**
   * @param index a transit service without real-time data, the cache is built from the scheduled
   *              trips only.
   */
  public static SiriFuzzyTripMatcherCache create(TransitService index) {
    var internalPlanningCodes = ImmutableSetMultimap.<String, Trip>builder();
    var lastStopArrivals = new HashMap<String, NavigableMap<Integer, Set<Trip>>>();

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

      var tripTimes = tripPattern.getScheduledTimetable().getTripTimes(trip);
      if (tripTimes != null) {
        int arrivalTime = tripTimes.getArrivalTime(tripTimes.getNumStops() - 1);
        lastStopArrivals
          .computeIfAbsent(lastStopId, k -> new TreeMap<>())
          .computeIfAbsent(arrivalTime, k -> new LinkedHashSet<>())
          .add(trip);
      }
    }

    var cache = new SiriFuzzyTripMatcherCache(
      internalPlanningCodes.build(),
      Map.copyOf(lastStopArrivals)
    );

    LOG.info(
      "Built internalPlanningCode-cache [{}].",
      cache.internalPlanningCodeCache.keySet().size()
    );
    LOG.info("Built last-stop-arrival-cache [{}].", cache.lastStopArrivalCache.size());
    return cache;
  }

  /**
   * The rail trips with the given NeTEx internal planning code, or an empty set if there are none.
   */
  public Set<Trip> tripsByInternalPlanningCode(String internalPlanningCode) {
    return internalPlanningCodeCache.get(internalPlanningCode);
  }

  /**
   * The trips whose scheduled last stop is the given stop and whose scheduled arrival time at
   * that stop is within the given window, or an empty set if there are none. All times are in
   * seconds since start of the service day and both limits are inclusive.
   */
  public Set<Trip> tripsByLastStopArrival(String stopId, int fromArrivalTime, int toArrivalTime) {
    var tripsByArrivalTime = lastStopArrivalCache.get(stopId);
    if (tripsByArrivalTime == null) {
      return Set.of();
    }
    var tripsInWindow = tripsByArrivalTime.subMap(fromArrivalTime, true, toArrivalTime, true);
    if (tripsInWindow.isEmpty()) {
      return Set.of();
    }
    var result = ImmutableSet.<Trip>builder();
    tripsInWindow.values().forEach(result::addAll);
    return result.build();
  }
}
