package org.opentripplanner.ext.siri;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.DateTimeHelper;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.RealtimeTestData;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;

public class SiriRealtimeTestEnvironment {

  public final RealtimeTestData testData = new RealtimeTestData();

  private static final TimetableSnapshotSourceParameters PARAMETERS = new TimetableSnapshotSourceParameters(
    Duration.ZERO,
    false
  );
  private final SiriTimetableSnapshotSource snapshotSource;
  private final DateTimeHelper dateTimeHelper;

  public SiriRealtimeTestEnvironment() {
    snapshotSource = new SiriTimetableSnapshotSource(PARAMETERS, testData.transitModel);
    dateTimeHelper = new DateTimeHelper(testData.timeZone, RealtimeTestData.SERVICE_DATE);
  }

  public EntityResolver getEntityResolver() {
    return new EntityResolver(testData.getTransitService(), testData.getFeedId());
  }

  public TripPattern getPatternForTrip(FeedScopedId tripId) {
    return getPatternForTrip(tripId, RealtimeTestData.SERVICE_DATE);
  }

  public TripPattern getPatternForTrip(FeedScopedId tripId, LocalDate serviceDate) {
    var transitService = testData.getTransitService();
    var trip = transitService.getTripOnServiceDateById(tripId);
    return transitService.getPatternForTrip(trip.getTrip(), serviceDate);
  }

  /**
   * Find the current TripTimes for a trip id on the default serviceDate
   */
  public TripTimes getTripTimesForTrip(Trip trip) {
    return testData.getTripTimesForTrip(trip.getId(), RealtimeTestData.SERVICE_DATE);
  }

  /**
   * Find the current TripTimes for a trip id on the default serviceDate
   */
  public TripTimes getTripTimesForTrip(String id) {
    return testData.getTripTimesForTrip(testData.id(id), RealtimeTestData.SERVICE_DATE);
  }

  public UpdateResult applyEstimatedTimetable(List<EstimatedTimetableDeliveryStructure> updates) {
    return snapshotSource.applyEstimatedTimetable(
      null,
      getEntityResolver(),
      testData.getFeedId(),
      false,
      updates
    );
  }

  public UpdateResult applyEstimatedTimetableWithFuzzyMatcher(
    List<EstimatedTimetableDeliveryStructure> updates
  ) {
    SiriFuzzyTripMatcher siriFuzzyTripMatcher = new SiriFuzzyTripMatcher(
      testData.getTransitService()
    );
    return applyEstimatedTimetable(updates, siriFuzzyTripMatcher);
  }

  public DateTimeHelper getDateTimeHelper() {
    return dateTimeHelper;
  }

  private UpdateResult applyEstimatedTimetable(
    List<EstimatedTimetableDeliveryStructure> updates,
    SiriFuzzyTripMatcher siriFuzzyTripMatcher
  ) {
    return this.snapshotSource.applyEstimatedTimetable(
        siriFuzzyTripMatcher,
        getEntityResolver(),
        testData.getFeedId(),
        false,
        updates
      );
  }
}
