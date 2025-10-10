package org.opentripplanner.transit.model._data;

import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import java.time.LocalDate;
import java.time.ZoneId;
import org.opentripplanner.LocalTimeParser;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.TimetableSnapshotParameters;
import org.opentripplanner.updater.trip.TimetableSnapshotManager;

/**
 * A helper class for setting up and interacting with transit data for tests.
 * <p>
 * The builder is used to create a SiteRepository and a TimetableRepository that can then be queried
 * by a TransitService.
 */
public final class TransitTestEnvironment {

  private final TimetableRepository timetableRepository;
  private final TimetableSnapshotManager snapshotManager;
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
  }

  /**
   * The default service date is the same as the date used by the builder when creating trips when
   * no explicit date is specified.
   */
  public LocalDate defaultServiceDate() {
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
  public TransitService transitService() {
    return new DefaultTransitService(timetableRepository, snapshotManager.getTimetableSnapshot());
  }

  public String feedId() {
    return FeedScopedIdForTestFactory.FEED_ID;
  }

  public TimetableRepository timetableRepository() {
    return timetableRepository;
  }

  public TimetableSnapshotManager timetableSnapshotManager() {
    return snapshotManager;
  }

  public TimetableSnapshot getTimetableSnapshot() {
    return snapshotManager.getTimetableSnapshot();
  }

  /**
   * A parser for converting local times into absolute times on the default service date in the
   * TransitService timezone.
   */
  public LocalTimeParser localTimeParser() {
    return new LocalTimeParser(timetableRepository.getTimeZone(), defaultServiceDate);
  }

  /**
   * Get a data fetcher for the given trip id on the default ServiceDate
   */
  public TripOnDateDataFetcher tripData(String tripId) {
    return new TripOnDateDataFetcher(transitService(), id(tripId), defaultServiceDate);
  }

  /**
   * Get a data fetcher for the given trip id on the default ServiceDate
   */
  public TripOnDateDataFetcher tripData(String tripId, LocalDate serviceDate) {
    return new TripOnDateDataFetcher(transitService(), id(tripId), serviceDate);
  }
}
