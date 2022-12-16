package org.opentripplanner.gtfs;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.TripDegenerate;
import org.opentripplanner.graph_builder.issues.TripUndefinedService;
import org.opentripplanner.graph_builder.module.geometry.GeometryProcessor;
import org.opentripplanner.model.Frequency;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.FrequencyEntry;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for generating trip patterns when loading GTFS data.
 */
public class GenerateTripPatternsOperation {

  private static final Logger LOG = LoggerFactory.getLogger(GenerateTripPatternsOperation.class);
  private final OtpTransitServiceBuilder transitDaoBuilder;
  private final DataImportIssueStore issueStore;
  private final Deduplicator deduplicator;
  private final Set<FeedScopedId> calendarServiceIds;
  private GeometryProcessor geometryProcessor;

  private final TripPatternCache tripPatternCache;
  private final ListMultimap<Trip, Frequency> frequenciesForTrip = ArrayListMultimap.create();

  private int tripCount = 0;
  private int freqCount = 0;
  private int scheduledCount = 0;

  // RT might prefix pattern with another suffix
  private static final String DEFAULT_PATTERN_ID_SUFFIX = "";

  public GenerateTripPatternsOperation(
    OtpTransitServiceBuilder builder,
    DataImportIssueStore issueStore,
    Deduplicator deduplicator,
    Set<FeedScopedId> calendarServiceIds,
    GeometryProcessor geometryProcessor
  ) {
    this.transitDaoBuilder = builder;
    this.issueStore = issueStore;
    this.deduplicator = deduplicator;
    this.calendarServiceIds = calendarServiceIds;
    this.geometryProcessor = geometryProcessor;
    this.tripPatternCache =
      new TripPatternCache(DEFAULT_PATTERN_ID_SUFFIX, transitDaoBuilder.getTripPatterns());
  }

  public void run() {
    collectFrequencyByTrip();

    final Collection<Trip> trips = transitDaoBuilder.getTripsById().values();
    final int tripsSize = trips.size();

    /* Loop over all trips, handling each one as a frequency-based or scheduled trip. */
    for (Trip trip : trips) {
      if (++tripCount % 100000 == 0) {
        LOG.debug("build trip patterns {}/{}", tripCount, tripsSize);
      }

      buildTripPatternForTrip(trip);
    }

    LOG.info(
      "Added {} frequency-based and {} single-trip timetable entries.",
      freqCount,
      scheduledCount
    );
  }

  public boolean hasFrequencyBasedTrips() {
    return freqCount > 0;
  }

  public boolean hasScheduledTrips() {
    return scheduledCount > 0;
  }

  /**
   * First, record which trips are used by one or more frequency entries. These trips will be
   * ignored for the purposes of non-frequency routing, and all the frequency entries referencing
   * the same trip can be added at once to the same Timetable/TripPattern.
   */
  private void collectFrequencyByTrip() {
    for (Frequency freq : transitDaoBuilder.getFrequencies()) {
      frequenciesForTrip.put(freq.getTrip(), freq);
    }
  }

  private void buildTripPatternForTrip(Trip trip) {
    // TODO: move to a validator module
    if (!calendarServiceIds.contains(trip.getServiceId())) {
      issueStore.add(new TripUndefinedService(trip));
      return; // Invalid trip, skip it, it will break later
    }

    List<StopTime> stopTimes = transitDaoBuilder.getStopTimesSortedByTrip().get(trip);

    // If after filtering this trip does not contain at least 2 stoptimes, it does not serve any purpose.
    var staticTripWithFewerThan2Stops =
      !FlexTrip.containsFlexStops(stopTimes) && stopTimes.size() < 2;
    // flex trips are allowed to have a single stop because that can be an area or a group of stops
    var flexTripWithZeroStops = FlexTrip.containsFlexStops(stopTimes) && stopTimes.size() < 1;
    if (staticTripWithFewerThan2Stops || flexTripWithZeroStops) {
      issueStore.add(new TripDegenerate(trip));
      return;
    }

    // Get the existing TripPattern for this filtered StopPattern, if exists
    // Or create one
    StopPattern stopPattern = new StopPattern(stopTimes);
    TripPattern tripPattern = getOrCreateTripPattern(stopPattern, trip);

    // Create a TripTimes object for this list of stoptimes, which form one trip.
    TripTimes tripTimes = new TripTimes(trip, stopTimes, deduplicator);

    // If this trip is referenced by one or more lines in frequencies.txt, wrap it in a FrequencyEntry.
    List<Frequency> frequencies = frequenciesForTrip.get(trip);
    if (!frequencies.isEmpty()) {
      for (Frequency freq : frequencies) {
        tripPattern.add(new FrequencyEntry(freq, tripTimes));
        freqCount++;
      }
    }
    // This trip was not frequency-based. Add the TripTimes directly to the TripPattern's scheduled timetable.
    else {
      tripPattern.add(tripTimes);
      scheduledCount++;
    }
  }

  private TripPattern getOrCreateTripPattern(StopPattern stopPattern, Trip trip) {
    TripPattern pattern = tripPatternCache.getTripPattern(stopPattern, trip);

    if (pattern == null) {
      FeedScopedId patternId = tripPatternCache.generateUniqueIdForTripPattern(trip);
      pattern =
        TripPattern
          .of(patternId)
          .withRoute(trip.getRoute())
          .withStopPattern(stopPattern)
          .withMode(trip.getMode())
          .withHopGeometries(geometryProcessor.createHopGeometries(trip))
          .build();

      tripPatternCache.add(stopPattern, pattern);
    }

    return pattern;
  }
}
