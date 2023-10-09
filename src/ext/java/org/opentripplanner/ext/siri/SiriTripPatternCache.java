package org.opentripplanner.ext.siri;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.network.TripPatternBuilder;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A synchronized cache of trip patterns that are added to the graph due to GTFS-realtime messages.
 */
public class SiriTripPatternCache {

  private static final Logger log = LoggerFactory.getLogger(SiriTripPatternCache.class);

  private final Map<StopPatternServiceDateKey, TripPattern> cache = new HashMap<>();

  private final ListMultimap<StopLocation, TripPattern> patternsForStop = Multimaps.synchronizedListMultimap(
    ArrayListMultimap.create()
  );

  private final Map<TripServiceDateKey, TripPattern> updatedTripPatternsForTripCache = new HashMap<>();

  private final SiriTripPatternIdGenerator tripPatternIdGenerator;
  private final Function<Trip, TripPattern> getPatternForTrip;

  public SiriTripPatternCache(
    SiriTripPatternIdGenerator tripPatternIdGenerator,
    Function<Trip, TripPattern> getPatternForTrip
  ) {
    this.tripPatternIdGenerator = tripPatternIdGenerator;
    this.getPatternForTrip = getPatternForTrip;
  }

  /**
   * Get cached trip pattern or create one if it doesn't exist yet. If a trip pattern is created,
   * vertices and edges for this trip pattern are also created in the transitModel.
   *
   * @param stopPattern stop pattern to retrieve/create trip pattern
   * @param trip        Trip containing route of new trip pattern in case a new trip pattern will be
   *                    created
   * @return cached or newly created trip pattern
   */
  public synchronized TripPattern getOrCreateTripPattern(
    @Nonnull final StopPattern stopPattern,
    @Nonnull final Trip trip,
    @Nonnull LocalDate serviceDate
  ) {
    TripPattern originalTripPattern = getPatternForTrip.apply(trip);

    if (originalTripPattern.getStopPattern().equals(stopPattern)) {
      return originalTripPattern;
    }

    // Check cache for trip pattern
    StopPatternServiceDateKey key = new StopPatternServiceDateKey(stopPattern, serviceDate);
    TripPattern tripPattern = cache.get(key);

    // Create TripPattern if it doesn't exist yet
    if (tripPattern == null) {
      var id = tripPatternIdGenerator.generateUniqueTripPatternId(trip);
      TripPatternBuilder tripPatternBuilder = TripPattern
        .of(id)
        .withRoute(trip.getRoute())
        .withMode(trip.getMode())
        .withNetexSubmode(trip.getNetexSubMode())
        .withStopPattern(stopPattern);

      // TODO - SIRI: Add pattern to transitModel index?

      tripPatternBuilder.withCreatedByRealtimeUpdater(true);
      tripPatternBuilder.withOriginalTripPattern(originalTripPattern);

      tripPattern = tripPatternBuilder.build();

      // Add pattern to cache
      cache.put(key, tripPattern);
    }

    /**
     *
     * When the StopPattern is first modified (e.g. change of platform), then updated (or vice versa), the stopPattern is altered, and
     * the StopPattern-object for the different states will not be equal.
     *
     * This causes both tripPatterns to be added to all unchanged stops along the route, which again causes duplicate results
     * in departureRow-searches (one departure for "updated", one for "modified").
     *
     * Full example:
     *      Planned stops: Stop 1 - Platform 1, Stop 2 - Platform 1
     *
     *      StopPattern #rt1: "updated" stopPattern cached in 'patternsForStop':
     *          - Stop 1, Platform 1
     *          	- StopPattern #rt1
     *          - Stop 2, Platform 1
     *          	- StopPattern #rt1
     *
     *      "modified" stopPattern: Stop 1 - Platform 1, Stop 2 - Platform 2
     *
     *      StopPattern #rt2: "modified" stopPattern cached in 'patternsForStop' will then be:
     *          - Stop 1, Platform 1
     *          	- StopPattern #rt1, StopPattern #rt2
     *          - Stop 2, Platform 1
     *          	- StopPattern #rt1
     *          - Stop 2, Platform 2
     *          	- StopPattern #rt2
     *
     *
     * Therefore, we must cleanup the duplicates by deleting the previously added (and thus outdated)
     * tripPattern for all affected stops. In example above, "StopPattern #rt1" should be removed from all stops
     *
     */
    TripServiceDateKey tripServiceDateKey = new TripServiceDateKey(trip, serviceDate);
    if (updatedTripPatternsForTripCache.containsKey(tripServiceDateKey)) {
      // Remove previously added TripPatterns for the trip currently being updated - if the stopPattern does not match
      TripPattern cachedTripPattern = updatedTripPatternsForTripCache.get(tripServiceDateKey);
      if (cachedTripPattern != null && !tripPattern.stopPatternIsEqual(cachedTripPattern)) {
        int sizeBefore = patternsForStop.values().size();
        long t1 = System.currentTimeMillis();
        patternsForStop.values().removeAll(Arrays.asList(cachedTripPattern));
        int sizeAfter = patternsForStop.values().size();

        log.debug(
          "Removed outdated TripPattern for {} stops in {} ms - tripId: {}",
          (sizeBefore - sizeAfter),
          (System.currentTimeMillis() - t1),
          trip.getId()
        );
        /*
                  TODO: Also remove previously updated - now outdated - TripPattern from cache ?
                  cache.remove(new StopPatternServiceDateKey(cachedTripPattern.stopPattern, serviceDate));
                */
      }
    }

    // To make these trip patterns visible for departureRow searches.
    for (var stop : tripPattern.getStops()) {
      if (!patternsForStop.containsEntry(stop, tripPattern)) {
        patternsForStop.put(stop, tripPattern);
      }
    }

    // Cache the last added tripPattern that has been used to update a specific trip
    updatedTripPatternsForTripCache.put(tripServiceDateKey, tripPattern);

    return tripPattern;
  }

  /**
   * Returns any new TripPatterns added by real time information for a given stop.
   *
   * @param stop the stop
   * @return list of TripPatterns created by real time sources for the stop.
   */
  public List<TripPattern> getAddedTripPatternsForStop(RegularStop stop) {
    return patternsForStop.get(stop);
  }
}

class StopPatternServiceDateKey {

  StopPattern stopPattern;
  LocalDate serviceDate;

  public StopPatternServiceDateKey(StopPattern stopPattern, LocalDate serviceDate) {
    this.stopPattern = stopPattern;
    this.serviceDate = serviceDate;
  }

  @Override
  public int hashCode() {
    return stopPattern.hashCode() + serviceDate.hashCode();
  }

  @Override
  public boolean equals(Object thatObject) {
    if (!(thatObject instanceof StopPatternServiceDateKey)) {
      return false;
    }
    StopPatternServiceDateKey that = (StopPatternServiceDateKey) thatObject;
    return (this.stopPattern.equals(that.stopPattern) && this.serviceDate.equals(that.serviceDate));
  }
}

class TripServiceDateKey {

  Trip trip;
  LocalDate serviceDate;

  public TripServiceDateKey(Trip trip, LocalDate serviceDate) {
    this.trip = trip;
    this.serviceDate = serviceDate;
  }

  @Override
  public int hashCode() {
    return trip.hashCode() + serviceDate.hashCode();
  }

  @Override
  public boolean equals(Object thatObject) {
    if (!(thatObject instanceof TripServiceDateKey)) {
      return false;
    }
    TripServiceDateKey that = (TripServiceDateKey) thatObject;
    return (this.trip.equals(that.trip) && this.serviceDate.equals(that.serviceDate));
  }
}
