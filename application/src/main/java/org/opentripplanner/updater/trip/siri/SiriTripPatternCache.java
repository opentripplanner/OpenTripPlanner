package org.opentripplanner.updater.trip.siri;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;

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
 *             be part of the main model - not a separate cache. It is possible that this class works when it comes to
 *             the thread-safety, but just by looking at a few lines of code I see problems - a strategy needs to be
 *             analysed, designed and documented.
 */
class SiriTripPatternCache {

  /**
   * We cache the trip pattern based on the stop pattern only in order to de-duplicate them.
   * <p>
   * Note that we don't really have a definition which properties are really part of the trip
   * pattern and several pattern keys are used in different parts of OTP.
   */
  private final Map<StopPattern, TripPattern> cache = new HashMap<>();

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
    final StopPattern stopPattern,
    final Trip trip
  ) {
    TripPattern originalTripPattern = getPatternForTrip.apply(trip);

    // TODO RT_AB: Verify implementation, which is different than the GTFS-RT version.
    //   It can return a TripPattern from the scheduled data, but protective copies are handled in
    //   TimetableSnapshot.update. Better document this aspect of the contract in this method's Javadoc.
    if (originalTripPattern.getStopPattern().equals(stopPattern)) {
      return originalTripPattern;
    }

    // Check cache for trip pattern
    TripPattern tripPattern = cache.get(stopPattern);

    // Create TripPattern if it doesn't exist yet
    if (tripPattern == null) {
      var id = tripPatternIdGenerator.generateUniqueTripPatternId(trip);
      tripPattern = TripPattern.of(id)
        .withRoute(trip.getRoute())
        .withMode(trip.getMode())
        .withNetexSubmode(trip.getNetexSubMode())
        .withStopPattern(stopPattern)
        .withCreatedByRealtimeUpdater(true)
        .withOriginalTripPattern(originalTripPattern)
        .build();
      // TODO: Add pattern to timetableRepository index?

      // Add pattern to cache
      cache.put(stopPattern, tripPattern);
    }

    return tripPattern;
  }
}
