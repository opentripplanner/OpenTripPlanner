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
 * See additional comments in the Javadoc of the GTFS-RT version of this class, whose name is
 * simply TripPatternCache.
 * TODO RT_AB: To the extent that double SIRI/GTFS implementations are kept, prefix all names
 *             with GTFS or SIRI or NETEX rather than having no prefix on the GTFS versions.
 *  TODO RT_TG: There is no clear strategy for what should be in the cache and the transit model and the flow
 *             between them. The NeTEx and a GTFS version of this should be merged. Having NeTex and GTFS
 *             specific indexes inside is ok. With the increased usage of DatedServiceJourneys, this should probably
 *             be part of the main model - not a separate cashe. It is possible that this class works when it comes to
 *             the thread-safety, but just by looking at a few lines of code I see problems - a strategy needs to be
 *             analysed, designed and documented.
 */
public class SiriTripPatternCache {

  private static final Logger LOG = LoggerFactory.getLogger(SiriTripPatternCache.class);

  // TODO RT_AB: Improve documentation. This seems to be the primary collection of added
  //   TripPatterns, with other collections serving as indexes. Similar to TripPatternCache.cache
  //   in the GTFS version of this class, but with service date as part of the key.
  private final Map<StopPatternServiceDateKey, TripPattern> cache = new HashMap<>();

  // TODO RT_AB: Improve documentation. This field appears to be an index that exists only in the
  //   SIRI version of this class (i.e. this version and not the older TripPatternCache that
  //   handles GTFS-RT). This index appears to be tailored for use by the Transmodel GraphQL APIs.
  private final ListMultimap<StopLocation, TripPattern> patternsForStop = Multimaps.synchronizedListMultimap(
    ArrayListMultimap.create()
  );

  // TODO RT_AB: clarify name and add documentation to this field.
  private final Map<TripServiceDateKey, TripPattern> updatedTripPatternsForTripCache = new HashMap<>();

  // TODO RT_AB: generalize this so we can generate IDs for SIRI or GTFS-RT sources.
  private final SiriTripPatternIdGenerator tripPatternIdGenerator;

  private final Function<Trip, TripPattern> getPatternForTrip;

  /**
   * TODO RT_AB: This class could potentially be reused for both SIRI and GTFS-RT, which may
   *   involve injecting a different ID generator and pattern fetching method.
   *
   * @param getPatternForTrip SiriTripPatternCache needs only this one feature of TransitService, so we retain
   *                          only this function reference to effectively narrow the interface. This should also facilitate
   *                          testing.
   */
  public SiriTripPatternCache(
    SiriTripPatternIdGenerator tripPatternIdGenerator,
    Function<Trip, TripPattern> getPatternForTrip
  ) {
    this.tripPatternIdGenerator = tripPatternIdGenerator;
    this.getPatternForTrip = getPatternForTrip;
  }

  /**
   * Get cached trip pattern or create one if it doesn't exist yet.
   *
   * TODO RT_AB: Improve documentation and/or merge with GTFS version of this class.
   *  This was clearly derived from a method from TripPatternCache. This is the only non-dead-code
   *  public method on this class, and mirrors the only public method on the GTFS-RT version of
   *  TripPatternCache. It also explains why this class is called a "cache". It allows reusing the
   *  same TripPattern instance when many different trips are created or updated with the same pattern.
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

    // TODO RT_AB: Verify implementation, which is different than the GTFS-RT version.
    //   It can return a TripPattern from the scheduled data, but protective copies are handled in
    //   TimetableSnapshot.update. Better document this aspect of the contract in this method's Javadoc.
    if (originalTripPattern.getStopPattern().equals(stopPattern)) {
      return originalTripPattern;
    }

    // Check cache for trip pattern
    StopPatternServiceDateKey key = new StopPatternServiceDateKey(stopPattern, serviceDate);
    TripPattern tripPattern = cache.get(key);

    // Create TripPattern if it doesn't exist yet
    if (tripPattern == null) {
      var id = tripPatternIdGenerator.generateUniqueTripPatternId(trip);
      tripPattern =
        TripPattern
          .of(id)
          .withRoute(trip.getRoute())
          .withMode(trip.getMode())
          .withNetexSubmode(trip.getNetexSubMode())
          .withStopPattern(stopPattern)
          .withCreatedByRealtimeUpdater(true)
          .withOriginalTripPattern(originalTripPattern)
          .build();
      // TODO: Add pattern to transitModel index?

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

     TODO RT_AB: review why this particular case is handled in an ad-hoc manner. It seems like all
       such indexes should be constantly rebuilt and versioned along with the TimetableSnapshot.
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
   * TODO RT_AB: this appears to be currently unused. Perhaps remove it if the API has changed.
   *
   * @param stop the stop
   * @return list of TripPatterns created by real time sources for the stop.
   */
  public List<TripPattern> getAddedTripPatternsForStop(RegularStop stop) {
    return patternsForStop.get(stop);
  }
}

// TODO RT_AB: move the below classes inside the above class as private static inner classes.
//   Defining these additional classes in the same top-level class file is unconventional.

/**
 * Serves as the key for the collection of TripPatterns added by realtime messages.
 * Must define hashcode and equals to confer semantic identity.
 * TODO RT_AB: clarify why each date has a different TripPattern instead of a different Timetable.
 *   It seems like there's a separate TripPattern instance for each StopPattern and service date,
 *   rather a single TripPattern instance associated with a separate timetable for each date.
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
 * TODO RT_AB: verify whether one map is considered the definitive collection and the other an index.
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
