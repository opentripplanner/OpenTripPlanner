package org.opentripplanner.transit.model._data;

import java.time.LocalDate;
import java.time.ZoneId;
import org.opentripplanner.LocalTimeParser;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.TimetableSnapshotParameters;
import org.opentripplanner.updater.trip.TimetableSnapshotManager;

/**
 * A helper class for creating and fetching transit entities
 */
public final class TransitTestEnvironment {

  private static final String DEFAULT_FEED_ID_FOR_TEST = "F";

  private final TimetableRepository timetableRepository;
  private final TimetableSnapshotManager snapshotManager;
  private final LocalTimeParser localTimeParser;
  private final LocalDate defaultServiceDate;

  public static TransitTestEnvironmentBuilder of() {
    return new TransitTestEnvironmentBuilder(ZoneId.of("Europe/Paris"), LocalDate.of(2024, 5, 7));
  }

  public static TransitTestEnvironmentBuilder of(LocalDate serviceDate) {
    return new TransitTestEnvironmentBuilder(ZoneId.of("Europe/Paris"), serviceDate);
  }

  public static TransitTestEnvironmentBuilder of(LocalDate serviceDate, ZoneId timeZone) {
    return new TransitTestEnvironmentBuilder(timeZone, serviceDate);
  }

  TransitTestEnvironment(TimetableRepository timetableRepository, LocalDate defaultServiceDate) {
    this.timetableRepository = timetableRepository;

    this.timetableRepository.index();
    this.snapshotManager = new TimetableSnapshotManager(
      null,
      TimetableSnapshotParameters.PUBLISH_IMMEDIATELY,
      () -> defaultServiceDate
    );
    this.defaultServiceDate = defaultServiceDate;
    this.localTimeParser = new LocalTimeParser(
      timetableRepository.getTimeZone(),
      defaultServiceDate
    );
  }

  public LocalDate serviceDate() {
    return defaultServiceDate;
  }

  /**
   * Get the timezone of the timetable repository
   */
  public ZoneId timeZone() {
    return timetableRepository.getTimeZone();
  }

  /**
   * Returns a new fresh TransitService
   */
  public TransitService getTransitService() {
    return new DefaultTransitService(timetableRepository, snapshotManager.getTimetableSnapshot());
  }

  public String getFeedId() {
    return TimetableRepositoryForTest.FEED_ID;
  }

  public TimetableRepository timetableRepository() {
    return timetableRepository;
  }

  public TimetableSnapshotManager timetableSnapshotManager() {
    return snapshotManager;
  }

  public LocalTimeParser localTimeParser() {
    return localTimeParser;
  }

  public TimetableSnapshot getTimetableSnapshot() {
    return snapshotManager.getTimetableSnapshot();
  }

  /**
   * Get a data fetcher for the given trip id on the default ServiceDate
   */
  public TripOnDateDataFetcher tripFetcher(String tripId) {
    return new TripOnDateDataFetcher(getTransitService(), id(tripId), defaultServiceDate);
  }

  /**
   * Get a data fetcher for the given trip id on the default ServiceDate
   */
  public TripOnDateDataFetcher tripFetcher(String tripId, LocalDate serviceDate) {
    return new TripOnDateDataFetcher(getTransitService(), id(tripId), serviceDate);
  }

  /**
   * Creates a feedscooped id using the default feed id
   */
  public static FeedScopedId id(String id) {
    return new FeedScopedId(DEFAULT_FEED_ID_FOR_TEST, id);
  }
}
