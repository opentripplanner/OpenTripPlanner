package org.opentripplanner.updater.trip;

import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import org.opentripplanner.DateTimeHelper;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesStringBuilder;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.TimetableSnapshotParameters;
import org.opentripplanner.updater.trip.gtfs.GtfsRealTimeTripUpdateAdapter;

/**
 * This class exists so that you can share the data building logic for GTFS and Siri tests.
 */
public final class RealtimeTestEnvironment {

  private final TimetableRepository timetableRepository;
  private final TimetableSnapshotManager snapshotManager;
  private final DateTimeHelper dateTimeHelper;
  private final LocalDate serviceDate;
  private final ZoneId timeZone;

  public static RealtimeTestEnvironmentBuilder of() {
    return new RealtimeTestEnvironmentBuilder(
      "F",
      ZoneId.of("Europe/Paris"),
      LocalDate.of(2024, 5, 7)
    );
  }

  public static RealtimeTestEnvironmentBuilder of(LocalDate serviceDate) {
    return new RealtimeTestEnvironmentBuilder("F", ZoneId.of("Europe/Paris"), serviceDate);
  }

  public static RealtimeTestEnvironmentBuilder of(LocalDate serviceDate, ZoneId timeZone) {
    return new RealtimeTestEnvironmentBuilder("F", timeZone, serviceDate);
  }

  RealtimeTestEnvironment(
    TimetableRepository timetableRepository,
    LocalDate defaultServiceDate,
    ZoneId zoneId
  ) {
    this.timetableRepository = timetableRepository;

    this.timetableRepository.index();
    this.snapshotManager = new TimetableSnapshotManager(
      null,
      TimetableSnapshotParameters.PUBLISH_IMMEDIATELY,
      () -> defaultServiceDate
    );
    this.timeZone = zoneId;
    this.serviceDate = defaultServiceDate;
    this.dateTimeHelper = new DateTimeHelper(zoneId, defaultServiceDate);
  }

  public LocalDate serviceDate() {
    return serviceDate;
  }

  public ZoneId timeZone() {
    return timeZone;
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

  public RegularStop getStop(String id) {
    return Objects.requireNonNull(timetableRepository.getSiteRepository().getRegularStop(id(id)));
  }

  public TimetableRepository timetableRepository() {
    return timetableRepository;
  }

  public TimetableSnapshotManager timetableSnapshotManager() {
    return snapshotManager;
  }

  public TripPattern getPatternForTrip(FeedScopedId tripId) {
    return getPatternForTrip(tripId, serviceDate);
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
    return getTripTimesForTrip(id(id), serviceDate);
  }

  public DateTimeHelper getDateTimeHelper() {
    return dateTimeHelper;
  }

  public TimetableSnapshot getTimetableSnapshot() {
    return snapshotManager.getTimetableSnapshot();
  }

  public String getRealtimeTimetable(String tripId) {
    return getRealtimeTimetable(id(tripId), serviceDate);
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
}
