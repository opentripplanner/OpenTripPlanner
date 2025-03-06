package org.opentripplanner.updater.trip.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.SiteRepository;
import uk.org.siri.siri20.OccupancyEnumeration;

public class TimetableHelperTest {

  private static final String FEED_ID = "FEED_ID";

  private static final String STOP_ID = "STOP_1";
  private static final String STATION_ID = "STOP_PARENT_1";
  private static final String STATION_NAME = "STATION_NAME_1";
  private static final String LINE_ID = "LINE_1";
  private static final String AGENCY_ID = "AGENCY_1";
  private static final String AGENCY_NAME = "AGENCY_ONE";
  private static final String LINE_SHORT_NAME = "LINE_ONE";

  private static final FeedScopedId SCOPED_STATION_ID = new FeedScopedId(FEED_ID, STATION_ID);
  private static final FeedScopedId SCOPED_STOP_ID = new FeedScopedId(FEED_ID, STOP_ID);
  private static final FeedScopedId SCOPED_AGENCY_ID = new FeedScopedId(FEED_ID, AGENCY_ID);
  private static final FeedScopedId SCOPED_LINE_ID = new FeedScopedId(FEED_ID, LINE_ID);
  private static final ZonedDateTime START_OF_SERVICE = ZonedDateTime.of(
    LocalDateTime.of(2022, 12, 9, 0, 0),
    ZoneIds.CET
  );

  private RealTimeTripTimes tripTimes;

  @BeforeEach
  public void setUp() {
    Station station = Station.of(SCOPED_STATION_ID)
      .withName(new NonLocalizedString(STATION_NAME))
      .withCoordinate(0.0, 0.0)
      .build();

    var stopTime = new StopTime();
    RegularStop stop = SiteRepository.of()
      .regularStop(SCOPED_STOP_ID)
      .withCoordinate(0.0, 0.0)
      .withParentStation(station)
      .build();
    stopTime.setStop(stop);

    Agency agency = Agency.of(SCOPED_AGENCY_ID).withName(AGENCY_NAME).withTimezone("CET").build();

    Route route = Route.of(SCOPED_LINE_ID)
      .withShortName(LINE_SHORT_NAME)
      .withAgency(agency)
      .withMode(TransitMode.FUNICULAR)
      .build();

    Trip trip = Trip.of(new FeedScopedId(FEED_ID, "TRIP_ID")).withRoute(route).build();
    tripTimes = TripTimesFactory.tripTimes(trip, List.of(stopTime), new Deduplicator());
  }

  @Test
  public void testApplyUpdates_MapPredictionInaccurate_EstimatedCall() {
    // Arrange

    CallWrapper estimatedCall = TestCall.of()
      .withStopPointRef(STOP_ID)
      .withCancellation(false)
      .withOccupancy(OccupancyEnumeration.SEATS_AVAILABLE)
      .withPredictionInaccurate(true)
      .build();

    // Act
    TimetableHelper.applyUpdates(START_OF_SERVICE, tripTimes, 0, false, false, estimatedCall, null);

    // Assert
    assertStatuses(0, OccupancyStatus.MANY_SEATS_AVAILABLE, false, false, true);
  }

  @Test
  public void testApplyUpdates_CancellationPriorityOverPredictionInaccurate_EstimatedCall() {
    // Arrange

    CallWrapper estimatedCall = TestCall.of()
      .withStopPointRef(STOP_ID)
      .withCancellation(true)
      .withOccupancy(OccupancyEnumeration.FULL)
      .withPredictionInaccurate(true)
      .build();

    // Act
    TimetableHelper.applyUpdates(START_OF_SERVICE, tripTimes, 0, false, false, estimatedCall, null);

    // Assert

    assertStatuses(0, OccupancyStatus.FULL, true, false, false);
  }

  @Test
  public void testApplyUpdates_CancellationPriorityOverPredictionInaccurate_RecordedCall() {
    // Arrange

    ZonedDateTime actualTime = START_OF_SERVICE.plus(Duration.ofHours(1));
    CallWrapper recordedCall = TestCall.of()
      .withStopPointRef(STOP_ID)
      .withPredictionInaccurate(true)
      .withOccupancy(OccupancyEnumeration.FULL)
      .withCancellation(true)
      .withActualDepartureTime(actualTime)
      .build();

    // Act
    TimetableHelper.applyUpdates(START_OF_SERVICE, tripTimes, 0, false, false, recordedCall, null);

    // Assert

    assertStatuses(0, OccupancyStatus.FULL, true, false, false);
  }

  @Test
  public void testApplyUpdates_PredictionInaccuratePriorityOverRecorded() {
    // Arrange

    CallWrapper recordedCall = TestCall.of()
      .withStopPointRef(STOP_ID)
      .withPredictionInaccurate(true)
      .withOccupancy(OccupancyEnumeration.FULL)
      .withCancellation(false)
      .withActualDepartureTime(START_OF_SERVICE.plus(Duration.ofHours(1)))
      .build();

    // Act
    TimetableHelper.applyUpdates(START_OF_SERVICE, tripTimes, 0, false, false, recordedCall, null);

    // Assert
    assertStatuses(0, OccupancyStatus.FULL, false, false, true);
  }

  @Test
  public void testApplyUpdates_ActualTimeResultsInRecorded() {
    // Arrange

    CallWrapper recordedCall = TestCall.of()
      .withStopPointRef(STOP_ID)
      .withPredictionInaccurate(false)
      .withOccupancy(OccupancyEnumeration.STANDING_AVAILABLE)
      .withCancellation(false)
      .withActualDepartureTime(START_OF_SERVICE.plus(Duration.ofHours(1)))
      .build();

    // Act
    TimetableHelper.applyUpdates(START_OF_SERVICE, tripTimes, 0, false, false, recordedCall, null);

    // Assert
    assertStatuses(0, OccupancyStatus.STANDING_ROOM_ONLY, false, true, false);
  }

  @Test
  public void testApplyUpdates_JourneyDefaultValues() {
    // Arrange
    CallWrapper recordedCall = TestCall.of().withStopPointRef(STOP_ID).build();

    // Act
    TimetableHelper.applyUpdates(
      START_OF_SERVICE,
      tripTimes,
      0,
      false,
      true,
      recordedCall,
      OccupancyEnumeration.STANDING_AVAILABLE
    );

    // Assert
    assertStatuses(0, OccupancyStatus.STANDING_ROOM_ONLY, false, false, true);
  }

  private void assertStatuses(
    int index,
    OccupancyStatus occupancyStatus,
    boolean cancelled,
    boolean recorded,
    boolean predictionInaccurate
  ) {
    assertEquals(
      predictionInaccurate,
      tripTimes.isPredictionInaccurate(index),
      "Prediction inaccurate mapped incorrectly"
    );
    assertEquals(recorded, tripTimes.isRecordedStop(index), "Recorded status mapped incorrectly");
    assertEquals(
      occupancyStatus,
      tripTimes.getOccupancyStatus(index),
      "Occupancy should be mapped to " + occupancyStatus
    );
    assertEquals(cancelled, tripTimes.isCancelledStop(index));
  }
}
