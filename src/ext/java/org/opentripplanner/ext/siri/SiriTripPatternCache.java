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
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Threadsafe mechanism for tracking any TripPatterns added to the graph via SIRI realtime messages.
 * This tracks only patterns added by realtime messages, not ones that already existed from the
 * scheduled NeTEx. This is a "cache" in the sense that it will keep returning the same TripPattern
 * when presented with the same StopPattern, so if realtime messages add many trips passing through
 * the same sequence of stops, they will all end up on this same TripPattern.
 * <p>
 * Note that there are two versions of this class, this one for GTFS-RT and another for SIRI.
 * See additional comments in the Javadoc of the GTFS-RT version of this class.
 */
public class SiriTripPatternCache {

  private static final Logger LOG = LoggerFactory.getLogger(SiriTripPatternCache.class);

  // Seems to be the primary collection of added TripPatterns, with other collections serving as
  // indexes. Similar to TripPatternCache.cache but with service date as part of the key.
  private final Map<StopPatternServiceDateKey, TripPattern> cache = new HashMap<>();

  // Apparently a SIRI-specific index for use in GraphQL APIs (missing on GTFS-RT version).
  private final ListMultimap<StopLocation, TripPattern> patternsForStop = Multimaps.synchronizedListMultimap(
    ArrayListMultimap.create()
  );

  // TODO clarify name and add documentation to this field
  private final Map<TripServiceDateKey, TripPattern> updatedTripPatternsForTripCache = new HashMap<>();

  // TODO generalize this so we can generate IDs for SIRI or GTFS-RT sources
  private final SiriTripPatternIdGenerator tripPatternIdGenerator;

  // TODO clarify name and add documentation to this field, and why it's constructor injected
  private final Function<Trip, TripPattern> getPatternForTrip;

  /**
   * Constructor.
   * TODO: clarify why the ID generator and pattern fetching function are injected. Potentially
   *     make the class usable for GTFS-RT cases by injecting different ID generator etc.
   */
  public SiriTripPatternCache(
    SiriTripPatternIdGenerator tripPatternIdGenerator,
    Function<Trip, TripPattern> getPatternForTrip
  ) {
    this.tripPatternIdGenerator = tripPatternIdGenerator;
    this.getPatternForTrip = getPatternForTrip;
  }

  // Below was clearly derived from a method from TripPatternCache, down to the obsolete Javadoc
  // mentioning transit vertices and edges (which don't exist since raptor was adopted).
  // Note that this is the only non-dead-code public method on this class, and mirrors the only
  // public method on the GTFS-RT version of TripPatternCache.
  // It also explains why this class is called a "cache". It allows reusing the same TripPattern
  // instance when many different trips are created or updated with the same pattern.

  /**
   * Get cached trip pattern or create one if it doesn't exist yet.
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

    // TODO: verify, this is different than GTFS-RT version
    //   It can return a TripPattern from the scheduled data, but protective copies are handled
    //   in TimetableSnapshot.update. Document better this aspect of the contract in this method's Javadoc.
    if (originalTripPattern.getStopPattern().equals(stopPattern)) {
      return originalTripPattern;
    }

    // Check cache for trip pattern
    StopPatternServiceDateKey key = new StopPatternServiceDateKey(stopPattern, serviceDate);
    TripPattern tripPattern = cache.get(key);

    // Create TripPattern if it doesn't exist yet
    if (tripPattern == null) {
      var id = tripPatternIdGenerator.generateUniqueTripPatternId(trip);
      tripPattern = TripPattern
        .of(id)
        .withRoute(trip.getRoute())
        .withMode(trip.getMode())
        .withNetexSubmode(trip.getNetexSubMode())
        .withStopPattern(stopPattern)
        .withCreatedByRealtimeUpdater(true)
        .withOriginalTripPattern(originalTripPattern)
        .build();
      // TODO - SIRI: Add pattern to transitModel index?

      // Add pattern to cache
      cache.put(key, tripPattern);
    }

    /*
     When the StopPattern is first modified (e.g. change of platform), then updated (or vice
     versa), the stopPattern is altered, and the StopPattern-object for the different states will
     not be equal.

     This causes both tripPatterns to be added to all unchanged stops along the route, which again
     causes duplicate results in departureRow-searches (one departure for "updated", one for
     "modified").

     Full example:
          Planned stops: Stop 1 - Platform 1, Stop 2 - Platform 1

          StopPattern #rt1: "updated" stopPattern cached in 'patternsForStop':
              - Stop 1, Platform 1
              	- StopPattern #rt1
              - Stop 2, Platform 1
              	- StopPattern #rt1

          "modified" stopPattern: Stop 1 - Platform 1, Stop 2 - Platform 2

          StopPattern #rt2: "modified" stopPattern cached in 'patternsForStop' will then be:
              - Stop 1, Platform 1
              	- StopPattern #rt1, StopPattern #rt2
              - Stop 2, Platform 1
              	- StopPattern #rt1
              - Stop 2, Platform 2
              	- StopPattern #rt2

     Therefore, we must clean up the duplicates by deleting the previously added (and thus
     outdated) tripPattern for all affected stops. In example above, "StopPattern #rt1" should be
     removed from all stops.

     TODO explore why this particular case is handled in an ad-hoc manner. It seems like all such
       indexes should be constantly rebuilt and versioned along with the TimetableSnapshot.
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

        LOG.debug(
          "Removed outdated TripPattern for {} stops in {} ms - tripId: {}",
          (sizeBefore - sizeAfter),
          (System.currentTimeMillis() - t1),
          trip.getId()
        );
        // TODO: Also remove previously updated - now outdated - TripPattern from cache ?
        // cache.remove(new StopPatternServiceDateKey(cachedTripPattern.stopPattern, serviceDate));
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
   * TODO: this appears to be currently unused. Perhaps remove it if the API has changed.
   *
   * @param stop the stop
   * @return list of TripPatterns created by real time sources for the stop.
   */
  public List<TripPattern> getAddedTripPatternsForStop(RegularStop stop) {
    return patternsForStop.get(stop);
  }
}

//// Below here are multiple additional private classes defined in the same top-level class file.
//// TODO: move these private classes inside the above class as private static inner classes.

/**
 * Serves as the key for the collection of realtime-added TripPatterns.
 * Must define hashcode and equals to confer semantic identity.
 * It seems like there's a separate TripPattern instance for each StopPattern and service date,
 * rather a single TripPattern instance associated with a separate timetable for each date.
 * TODO: clarify why each date has a different TripPattern instead of a different Timetable.
 */
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

/**
 * An alternative key for looking up realtime-added TripPatterns by trip and service date instead
 * of stop pattern and service date. Must define hashcode and equals to confer semantic identity.
 * TODO verify whether one map is considered the definitive collection and the other an index.
 */
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
