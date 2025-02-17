package org.opentripplanner.updater.trip;

import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import com.google.transit.realtime.GtfsRealtime;
import java.time.LocalDate;
import java.util.List;
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
import org.opentripplanner.updater.TimetableSnapshotParameters;
import org.opentripplanner.updater.siri.SiriRealTimeTripUpdateAdapter;
import org.opentripplanner.updater.siri.updater.EstimatedTimetableHandler;
import org.opentripplanner.updater.spi.UpdateResult;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;

/**
 * This class exists so that you can share the data building logic for GTFS and Siri tests.
 */
public final class RealtimeTestEnvironment implements RealtimeTestConstants {

  public final TimetableRepository timetableRepository;
  public final TimetableSnapshotManager snapshotManager;
  private final SiriRealTimeTripUpdateAdapter siriAdapter;
  private final GtfsRealTimeTripUpdateAdapter gtfsAdapter;
  private final DateTimeHelper dateTimeHelper;

  public static RealtimeTestEnvironmentBuilder of() {
    return new RealtimeTestEnvironmentBuilder();
  }

  RealtimeTestEnvironment(TimetableRepository timetableRepository) {
    this.timetableRepository = timetableRepository;

    this.timetableRepository.index();
    this.snapshotManager =
      new TimetableSnapshotManager(
        null,
        TimetableSnapshotParameters.PUBLISH_IMMEDIATELY,
        () -> SERVICE_DATE
      );
    siriAdapter = new SiriRealTimeTripUpdateAdapter(timetableRepository, snapshotManager);
    gtfsAdapter =
      new GtfsRealTimeTripUpdateAdapter(timetableRepository, snapshotManager, () -> SERVICE_DATE);
    dateTimeHelper = new DateTimeHelper(TIME_ZONE, SERVICE_DATE);
  }

  /**
   * Returns a new fresh TransitService
   */
  public TransitService getTransitService() {
    return new DefaultTransitService(timetableRepository, snapshotManager.getTimetableSnapshot());
  }

  /**
   * Find the current TripTimes for a trip id on a serviceDate
   */
  public TripTimes getTripTimesForTrip(FeedScopedId tripId, LocalDate serviceDate) {
    var transitService = getTransitService();
    var trip = transitService.getTripOnServiceDate(tripId).getTrip();
    var pattern = transitService.findPattern(trip, serviceDate);
    var timetable = transitService.findTimetable(pattern, serviceDate);
    return timetable.getTripTimes(trip);
  }

  public String getFeedId() {
    return TimetableRepositoryForTest.FEED_ID;
  }

  private EstimatedTimetableHandler getEstimatedTimetableHandler(boolean fuzzyMatching) {
    return new EstimatedTimetableHandler(siriAdapter, fuzzyMatching, getFeedId());
  }

  public TripPattern getPatternForTrip(FeedScopedId tripId) {
    return getPatternForTrip(tripId, SERVICE_DATE);
  }

  public TripPattern getPatternForTrip(String id) {
    return getPatternForTrip(id(id));
  }

  public TripPattern getPatternForTrip(FeedScopedId tripId, LocalDate serviceDate) {
    var transitService = getTransitService();
    var trip = transitService.getTripOnServiceDate(tripId);
    return transitService.findPattern(trip.getTrip(), serviceDate);
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
    return snapshotManager.getTimetableSnapshot();
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
    UpdateResult updateResult = gtfsAdapter.applyTripUpdates(
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
    UpdateResult updateResult = getEstimatedTimetableHandler(fuzzyMatching)
      .applyUpdate(
        updates,
        DIFFERENTIAL,
        new DefaultRealTimeUpdateContext(
          new Graph(),
          timetableRepository,
          snapshotManager.getTimetableSnapshotBuffer()
        )
      );
    commitTimetableSnapshot();
    return updateResult;
  }

  private void commitTimetableSnapshot() {
    snapshotManager.purgeAndCommit();
  }
}
