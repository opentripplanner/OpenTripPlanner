package org.opentripplanner.updater.trip;

import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import com.google.transit.realtime.GtfsRealtime;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.DateTimeHelper;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesStringBuilder;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.DefaultRealTimeUpdateContext;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
import org.opentripplanner.updater.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.updater.siri.updater.EstimatedTimetableHandler;
import org.opentripplanner.updater.spi.UpdateResult;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;

/**
 * This class exists so that you can share the data building logic for GTFS and Siri tests.
 * Since it's not possible to add a Siri and GTFS updater to the transit model at the same time,
 * they each have their own test environment.
 * <p>
 * It is however a goal to change that and then these two can be combined.
 */
public final class RealtimeTestEnvironment implements RealtimeTestConstants {

  // static constants
  private static final TimetableSnapshotSourceParameters PARAMETERS = new TimetableSnapshotSourceParameters(
    Duration.ZERO,
    false
  );

  public final TimetableRepository timetableRepository;
  private final SiriTimetableSnapshotSource siriSource;
  private final TimetableSnapshotSource gtfsSource;
  private final DateTimeHelper dateTimeHelper;

  enum SourceType {
    GTFS_RT,
    SIRI,
  }

  /**
   * Siri and GTFS-RT cannot be run at the same time, so you need to decide.
   */
  public static RealtimeTestEnvironmentBuilder siri() {
    return new RealtimeTestEnvironmentBuilder().withSourceType(SourceType.SIRI);
  }

  /**
   * Siri and GTFS-RT cannot be run at the same time, so you need to decide.
   */
  public static RealtimeTestEnvironmentBuilder gtfs() {
    return new RealtimeTestEnvironmentBuilder().withSourceType(SourceType.GTFS_RT);
  }

  RealtimeTestEnvironment(SourceType sourceType, TimetableRepository timetableRepository) {
    Objects.requireNonNull(sourceType);
    this.timetableRepository = timetableRepository;

    this.timetableRepository.index();
    // SIRI and GTFS-RT cannot be registered with the transit model at the same time
    // we are actively refactoring to remove this restriction
    // for the time being you cannot run a SIRI and GTFS-RT test at the same time
    if (sourceType == SourceType.SIRI) {
      siriSource = new SiriTimetableSnapshotSource(PARAMETERS, timetableRepository);
      gtfsSource = null;
    } else {
      gtfsSource = new TimetableSnapshotSource(PARAMETERS, timetableRepository);
      siriSource = null;
    }
    dateTimeHelper = new DateTimeHelper(TIME_ZONE, SERVICE_DATE);
  }

  /**
   * Returns a new fresh TransitService
   */
  public TransitService getTransitService() {
    return new DefaultTransitService(timetableRepository);
  }

  /**
   * Find the current TripTimes for a trip id on a serviceDate
   */
  public TripTimes getTripTimesForTrip(FeedScopedId tripId, LocalDate serviceDate) {
    var transitService = getTransitService();
    var trip = transitService.getTripOnServiceDateById(tripId).getTrip();
    var pattern = transitService.getPatternForTrip(trip, serviceDate);
    var timetable = transitService.getTimetableForTripPattern(pattern, serviceDate);
    return timetable.getTripTimes(trip);
  }

  public String getFeedId() {
    return TimetableRepositoryForTest.FEED_ID;
  }

  private EstimatedTimetableHandler getEstimatedTimetableHandler(boolean fuzzyMatching) {
    return new EstimatedTimetableHandler(siriSource, fuzzyMatching, getFeedId());
  }

  public TripPattern getPatternForTrip(FeedScopedId tripId) {
    return getPatternForTrip(tripId, SERVICE_DATE);
  }

  public TripPattern getPatternForTrip(String id) {
    return getPatternForTrip(id(id));
  }

  public TripPattern getPatternForTrip(FeedScopedId tripId, LocalDate serviceDate) {
    var transitService = getTransitService();
    var trip = transitService.getTripOnServiceDateById(tripId);
    return transitService.getPatternForTrip(trip.getTrip(), serviceDate);
  }

  /**
   * Find the current TripTimes for a trip id on the default serviceDate
   */
  public TripTimes getTripTimesForTrip(String id) {
    return getTripTimesForTrip(id(id), SERVICE_DATE);
  }

  public DateTimeHelper getDateTimeHelper() {
    return dateTimeHelper;
  }

  public TimetableSnapshot getTimetableSnapshot() {
    if (siriSource != null) {
      return siriSource.getTimetableSnapshot();
    } else {
      return gtfsSource.getTimetableSnapshot();
    }
  }

  public String getRealtimeTimetable(String tripId) {
    return getRealtimeTimetable(id(tripId), SERVICE_DATE);
  }

  public String getRealtimeTimetable(FeedScopedId tripId, LocalDate serviceDate) {
    var tt = getTripTimesForTrip(tripId, serviceDate);
    var pattern = getPatternForTrip(tripId);

    return TripTimesStringBuilder.encodeTripTimes(tt, pattern);
  }

  public String getScheduledTimetable(String tripId) {
    return getScheduledTimetable(id(tripId));
  }

  public String getScheduledTimetable(FeedScopedId tripId) {
    var pattern = getPatternForTrip(tripId);
    var tt = pattern.getScheduledTimetable().getTripTimes(tripId);

    return TripTimesStringBuilder.encodeTripTimes(tt, pattern);
  }

  // SIRI updates

  public UpdateResult applyEstimatedTimetableWithFuzzyMatcher(
    List<EstimatedTimetableDeliveryStructure> updates
  ) {
    return applyEstimatedTimetable(updates, true);
  }

  public UpdateResult applyEstimatedTimetable(List<EstimatedTimetableDeliveryStructure> updates) {
    return applyEstimatedTimetable(updates, false);
  }

  // GTFS-RT updates

  public UpdateResult applyTripUpdate(GtfsRealtime.TripUpdate update) {
    return applyTripUpdates(List.of(update), FULL_DATASET);
  }

  public UpdateResult applyTripUpdate(
    GtfsRealtime.TripUpdate update,
    UpdateIncrementality incrementality
  ) {
    return applyTripUpdates(List.of(update), incrementality);
  }

  public UpdateResult applyTripUpdates(
    List<GtfsRealtime.TripUpdate> updates,
    UpdateIncrementality incrementality
  ) {
    Objects.requireNonNull(gtfsSource, "Test environment is configured for SIRI only");
    UpdateResult updateResult = gtfsSource.applyTripUpdates(
      null,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA,
      incrementality,
      updates,
      getFeedId()
    );
    commitTimetableSnapshot();
    return updateResult;
  }

  // private methods

  private UpdateResult applyEstimatedTimetable(
    List<EstimatedTimetableDeliveryStructure> updates,
    boolean fuzzyMatching
  ) {
    Objects.requireNonNull(siriSource, "Test environment is configured for GTFS-RT only");
    UpdateResult updateResult = getEstimatedTimetableHandler(fuzzyMatching)
      .applyUpdate(
        updates,
        DIFFERENTIAL,
        new DefaultRealTimeUpdateContext(
          new Graph(),
          timetableRepository,
          siriSource.getTimetableSnapshotBuffer()
        )
      );
    commitTimetableSnapshot();
    return updateResult;
  }

  private void commitTimetableSnapshot() {
    if (siriSource != null) {
      siriSource.flushBuffer();
    }
    if (gtfsSource != null) {
      gtfsSource.flushBuffer();
    }
  }
}
