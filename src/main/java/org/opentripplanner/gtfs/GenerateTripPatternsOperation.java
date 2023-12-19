package org.opentripplanner.gtfs;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.logging.ProgressTracker;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.TripDegenerate;
import org.opentripplanner.graph_builder.issues.TripUndefinedService;
import org.opentripplanner.graph_builder.module.geometry.GeometryProcessor;
import org.opentripplanner.model.Frequency;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.FrequencyEntry;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for generating trip patterns when loading GTFS data.
 */
public class GenerateTripPatternsOperation {

  private static final Logger LOG = LoggerFactory.getLogger(GenerateTripPatternsOperation.class);

  private final Map<String, Integer> tripPatternIdCounters = new HashMap<>();

  private final OtpTransitServiceBuilder transitDaoBuilder;
  private final DataImportIssueStore issueStore;
  private final Deduplicator deduplicator;
  private final Set<FeedScopedId> calendarServiceIds;
  private final GeometryProcessor geometryProcessor;

  private final Multimap<StopPattern, TripPattern> tripPatterns;
  private final ListMultimap<Trip, Frequency> frequenciesForTrip = ArrayListMultimap.create();

  private int freqCount = 0;
  private int scheduledCount = 0;

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
    this.tripPatterns = transitDaoBuilder.getTripPatterns();
  }

  public void run() {
    collectFrequencyByTrip();

    final Collection<Trip> trips = transitDaoBuilder.getTripsById().values();
    var progressLogger = ProgressTracker.track("build trip patterns", 50_000, trips.size());
    LOG.info(progressLogger.startMessage());

    /* Loop over all trips, handling each one as a frequency-based or scheduled trip. */
    for (Trip trip : trips) {
      try {
        buildTripPatternForTrip(trip);
        //noinspection Convert2MethodRef
        progressLogger.step(m -> LOG.info(m));
      } catch (DataValidationException e) {
        issueStore.add(e.error());
      }
    }

    LOG.info(progressLogger.completeMessage());
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

    // Get the existing TripPattern for this filtered StopPattern, or create one.
    StopPattern stopPattern = new StopPattern(stopTimes);

    Direction direction = trip.getDirection();
    TripPattern tripPattern = findOrCreateTripPattern(stopPattern, trip, direction);

    // Create a TripTimes object for this list of stoptimes, which form one trip.
    TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, deduplicator);

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

  private TripPattern findOrCreateTripPattern(
    StopPattern stopPattern,
    Trip trip,
    Direction direction
  ) {
    Route route = trip.getRoute();
    for (TripPattern tripPattern : tripPatterns.get(stopPattern)) {
      if (
        tripPattern.getRoute().equals(route) &&
        tripPattern.getDirection().equals(direction) &&
        tripPattern.getMode().equals(trip.getMode()) &&
        tripPattern.getNetexSubmode().equals(trip.getNetexSubMode())
      ) {
        return tripPattern;
      }
    }
    FeedScopedId patternId = generateUniqueIdForTripPattern(route, direction);
    TripPattern tripPattern = TripPattern
      .of(patternId)
      .withRoute(route)
      .withStopPattern(stopPattern)
      .withMode(trip.getMode())
      .withNetexSubmode(trip.getNetexSubMode())
      .withHopGeometries(geometryProcessor.createHopGeometries(trip))
      .build();
    tripPatterns.put(stopPattern, tripPattern);
    return tripPattern;
  }

  /**
   * Patterns do not have unique IDs in GTFS, so we make some by concatenating agency id, route id,
   * the direction and an integer. This only works if the Collection of TripPattern includes every
   * TripPattern for the agency.
   */
  private FeedScopedId generateUniqueIdForTripPattern(Route route, Direction direction) {
    FeedScopedId routeId = route.getId();
    String directionId = direction == Direction.UNKNOWN ? "" : Integer.toString(direction.gtfsCode);
    String key = routeId.getId() + ":" + direction;

    // Add 1 to counter and update it
    int counter = tripPatternIdCounters.getOrDefault(key, 0) + 1;
    tripPatternIdCounters.put(key, counter);

    String id = String.format("%s:%s:%02d", routeId.getId(), directionId, counter);

    return new FeedScopedId(routeId.getFeedId(), id);
  }
}
