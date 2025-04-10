package org.opentripplanner.updater.trip.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.spi.UpdateError;
import uk.org.siri.siri21.DepartureBoardingActivityEnumeration;

class ModifiedTripBuilderTest {

  /* Transit model */

  private static final Agency AGENCY = TimetableRepositoryForTest.AGENCY;
  private static final ZoneId TIME_ZONE = AGENCY.getTimezone();
  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();
  private static final Station STATION_A = TEST_MODEL.station("A").build();
  private static final Station STATION_B = TEST_MODEL.station("B").build();
  private static final Station STATION_C = TEST_MODEL.station("C").build();

  private static final RegularStop STOP_A_1 = TEST_MODEL.stop("A_1")
    .withParentStation(STATION_A)
    .build();
  private static final RegularStop STOP_A_2 = TEST_MODEL.stop("A_2")
    .withParentStation(STATION_A)
    .build();
  private static final RegularStop STOP_B_1 = TEST_MODEL.stop("B_1")
    .withParentStation(STATION_B)
    .build();
  private static final RegularStop STOP_C_1 = TEST_MODEL.stop("C_1")
    .withParentStation(STATION_C)
    .build();
  private static final RegularStop STOP_D = TEST_MODEL.stop("D").build();

  private static final Route ROUTE = TimetableRepositoryForTest.route("ROUTE_ID")
    .withAgency(AGENCY)
    .build();

  private static final TripPattern PATTERN = TimetableRepositoryForTest.tripPattern(
    "PATTERN_ID",
    ROUTE
  )
    .withStopPattern(TimetableRepositoryForTest.stopPattern(STOP_A_1, STOP_B_1, STOP_C_1))
    .build();

  private static final FeedScopedId SERVICE_ID = TimetableRepositoryForTest.id("CAL_1");

  private static final Trip TRIP = TimetableRepositoryForTest.trip("TRIP")
    .withRoute(ROUTE)
    .withServiceId(SERVICE_ID)
    .build();

  private static final Deduplicator DEDUPLICATOR = new Deduplicator();

  private static final StopTime STOP_TIME_A_1 = new StopTime();

  static {
    STOP_TIME_A_1.setStop(STOP_A_1);
    STOP_TIME_A_1.setArrivalTime(secondsInDay(10, 0));
    STOP_TIME_A_1.setDepartureTime(secondsInDay(10, 0));
    STOP_TIME_A_1.setStopSequence(0);
  }

  private static final StopTime STOP_TIME_B_1 = new StopTime();

  static {
    STOP_TIME_B_1.setStop(STOP_B_1);
    STOP_TIME_B_1.setArrivalTime(secondsInDay(10, 10));
    STOP_TIME_B_1.setDepartureTime(secondsInDay(10, 12));
    STOP_TIME_B_1.setStopSequence(1);
  }

  private static final StopTime STOP_TIME_C_1 = new StopTime();

  static {
    STOP_TIME_C_1.setStop(STOP_C_1);
    STOP_TIME_C_1.setArrivalTime(secondsInDay(10, 20));
    STOP_TIME_C_1.setDepartureTime(secondsInDay(10, 20));
    STOP_TIME_C_1.setStopSequence(1);
  }

  private static final TripTimes TRIP_TIMES = TripTimesFactory.tripTimes(
    TRIP,
    List.of(STOP_TIME_A_1, STOP_TIME_B_1, STOP_TIME_C_1),
    DEDUPLICATOR
  );

  private static final LocalDate SERVICE_DATE = LocalDate.of(2023, 2, 17);
  private final SiteRepository siteRepository = TEST_MODEL.siteRepositoryBuilder()
    .withRegularStop(STOP_A_1)
    .withRegularStop(STOP_A_2)
    .withRegularStop(STOP_B_1)
    .withRegularStop(STOP_C_1)
    .withRegularStop(STOP_D)
    .build();
  private final TimetableRepository timetableRepository = new TimetableRepository(
    siteRepository,
    DEDUPLICATOR
  );
  private EntityResolver entityResolver;

  @BeforeEach
  void setUp() {
    // Add entities to transit model for the entity resolver
    timetableRepository.addAgency(AGENCY);
    timetableRepository.addTripPattern(PATTERN.getId(), PATTERN);

    // Crate a scheduled calendar, to have the SERVICE_DATE be within the transit feed coverage
    CalendarServiceData calendarServiceData = new CalendarServiceData();
    calendarServiceData.putServiceDatesForServiceId(
      SERVICE_ID,
      List.of(SERVICE_DATE.minusDays(1), SERVICE_DATE, SERVICE_DATE.plusDays(1))
    );
    timetableRepository.getServiceCodes().put(SERVICE_ID, 0);
    timetableRepository.updateCalendarServiceData(
      true,
      calendarServiceData,
      DataImportIssueStore.NOOP
    );

    // Create transit model index
    timetableRepository.index();

    // Create the entity resolver only after the model has been indexed
    entityResolver = new EntityResolver(
      new DefaultTransitService(timetableRepository),
      TimetableRepositoryForTest.FEED_ID
    );
  }

  @Test
  void testUpdateNoCalls() {
    var result = new ModifiedTripBuilder(
      TRIP_TIMES,
      PATTERN,
      SERVICE_DATE,
      timetableRepository.getTimeZone(),
      entityResolver,
      List.of(),
      false,
      null,
      false,
      "DATASOURCE"
    ).build();

    assertTrue(result.isFailure());
    assertEquals(UpdateError.UpdateErrorType.TOO_FEW_STOPS, result.failureValue().errorType());
  }

  @Test
  void testUpdateCancellation() {
    var result = new ModifiedTripBuilder(
      TRIP_TIMES,
      PATTERN,
      SERVICE_DATE,
      timetableRepository.getTimeZone(),
      entityResolver,
      List.of(
        TestCall.of()
          .withStopPointRef(STOP_A_1.getId().getId())
          .withAimedDepartureTime(zonedDateTime(10, 0))
          .withExpectedDepartureTime(zonedDateTime(10, 1))
          .build(),
        TestCall.of()
          .withStopPointRef(STOP_B_1.getId().getId())
          .withAimedArrivalTime(zonedDateTime(10, 10))
          .withExpectedArrivalTime(zonedDateTime(10, 11))
          .withAimedDepartureTime(zonedDateTime(10, 12))
          .withExpectedDepartureTime(zonedDateTime(10, 13))
          .build(),
        TestCall.of()
          .withStopPointRef(STOP_C_1.getId().getId())
          .withAimedArrivalTime(zonedDateTime(10, 20))
          .withExpectedArrivalTime(zonedDateTime(10, 22))
          .build()
      ),
      true,
      null,
      false,
      "DATASOURCE"
    ).build();

    assertTrue(result.isSuccess(), "Update should succeed");

    TripUpdate tripUpdate = result.successValue();
    assertEquals(PATTERN.getStopPattern(), tripUpdate.stopPattern());
    TripTimes updatedTimes = tripUpdate.tripTimes();
    assertEquals(RealTimeState.CANCELED, updatedTimes.getRealTimeState());
  }

  @Test
  void testUpdateSameStops() {
    var result = new ModifiedTripBuilder(
      TRIP_TIMES,
      PATTERN,
      SERVICE_DATE,
      timetableRepository.getTimeZone(),
      entityResolver,
      List.of(
        TestCall.of()
          .withStopPointRef(STOP_A_1.getId().getId())
          .withAimedDepartureTime(zonedDateTime(10, 0))
          .withExpectedDepartureTime(zonedDateTime(10, 1))
          .build(),
        TestCall.of()
          .withStopPointRef(STOP_B_1.getId().getId())
          .withAimedArrivalTime(zonedDateTime(10, 10))
          .withExpectedArrivalTime(zonedDateTime(10, 11))
          .withAimedDepartureTime(zonedDateTime(10, 12))
          .withExpectedDepartureTime(zonedDateTime(10, 13))
          .build(),
        TestCall.of()
          .withStopPointRef(STOP_C_1.getId().getId())
          .withAimedArrivalTime(zonedDateTime(10, 20))
          .withExpectedArrivalTime(zonedDateTime(10, 22))
          .build()
      ),
      false,
      null,
      false,
      "DATASOURCE"
    ).build();

    assertTrue(result.isSuccess(), "Update should succeed");

    TripUpdate tripUpdate = result.successValue();
    assertEquals(PATTERN.getStopPattern(), tripUpdate.stopPattern());
    TripTimes updatedTimes = tripUpdate.tripTimes();
    assertEquals(secondsInDay(10, 1), updatedTimes.getArrivalTime(0));
    assertEquals(secondsInDay(10, 1), updatedTimes.getDepartureTime(0));
    assertEquals(secondsInDay(10, 11), updatedTimes.getArrivalTime(1));
    assertEquals(secondsInDay(10, 13), updatedTimes.getDepartureTime(1));
    assertEquals(secondsInDay(10, 22), updatedTimes.getArrivalTime(2));
    assertEquals(secondsInDay(10, 22), updatedTimes.getDepartureTime(2));
    assertEquals(RealTimeState.UPDATED, updatedTimes.getRealTimeState());
  }

  @Test
  void testUpdateValidationFailure() {
    var result = new ModifiedTripBuilder(
      TRIP_TIMES,
      PATTERN,
      SERVICE_DATE,
      timetableRepository.getTimeZone(),
      entityResolver,
      List.of(
        TestCall.of()
          .withStopPointRef(STOP_A_1.getId().getId())
          .withAimedDepartureTime(zonedDateTime(10, 0))
          .withExpectedDepartureTime(zonedDateTime(10, 1))
          .build(),
        TestCall.of()
          .withStopPointRef(STOP_B_1.getId().getId())
          .withAimedArrivalTime(zonedDateTime(10, 10))
          .withExpectedArrivalTime(zonedDateTime(10, 12))
          .withAimedDepartureTime(zonedDateTime(10, 12))
          .withExpectedDepartureTime(zonedDateTime(10, 10))
          .build(),
        TestCall.of()
          .withStopPointRef(STOP_C_1.getId().getId())
          .withAimedArrivalTime(zonedDateTime(10, 20))
          .withExpectedArrivalTime(zonedDateTime(10, 22))
          .build()
      ),
      false,
      null,
      false,
      "DATASOURCE"
    ).build();

    assertFalse(result.isSuccess(), "Update should fail");
    UpdateError updateError = result.failureValue();

    // Check that values are copied over
    assertEquals(UpdateError.UpdateErrorType.NEGATIVE_DWELL_TIME, updateError.errorType());
    assertEquals(1, updateError.stopIndex());
  }

  /**
   * This checks a problem, where the real-time departure time is earlier than the scheduled arrival
   * time, which would typically create a validation issue. It is however ok for the first stop.
   */
  @Test
  void testUpdateSameStopsDepartEarly() {
    var result = new ModifiedTripBuilder(
      TRIP_TIMES,
      PATTERN,
      SERVICE_DATE,
      timetableRepository.getTimeZone(),
      entityResolver,
      List.of(
        TestCall.of()
          .withStopPointRef(STOP_A_1.getId().getId())
          .withAimedDepartureTime(zonedDateTime(10, 0))
          .withExpectedDepartureTime(zonedDateTime(9, 58))
          .build(),
        TestCall.of()
          .withStopPointRef(STOP_B_1.getId().getId())
          .withAimedArrivalTime(zonedDateTime(10, 10))
          .withExpectedArrivalTime(zonedDateTime(10, 11))
          .withAimedDepartureTime(zonedDateTime(10, 12))
          .withExpectedDepartureTime(zonedDateTime(10, 13))
          .build(),
        TestCall.of()
          .withStopPointRef(STOP_C_1.getId().getId())
          .withAimedArrivalTime(zonedDateTime(10, 20))
          .withExpectedArrivalTime(zonedDateTime(10, 22))
          .build()
      ),
      false,
      null,
      false,
      "DATASOURCE"
    ).build();

    assertTrue(result.isSuccess(), "Update should succeed");

    TripUpdate tripUpdate = result.successValue();
    assertEquals(PATTERN.getStopPattern(), tripUpdate.stopPattern());
    TripTimes updatedTimes = tripUpdate.tripTimes();
    assertEquals(secondsInDay(9, 58), updatedTimes.getArrivalTime(0));
    assertEquals(secondsInDay(9, 58), updatedTimes.getDepartureTime(0));
    assertEquals(secondsInDay(10, 11), updatedTimes.getArrivalTime(1));
    assertEquals(secondsInDay(10, 13), updatedTimes.getDepartureTime(1));
    assertEquals(secondsInDay(10, 22), updatedTimes.getArrivalTime(2));
    assertEquals(secondsInDay(10, 22), updatedTimes.getDepartureTime(2));
    assertEquals(RealTimeState.UPDATED, updatedTimes.getRealTimeState());
  }

  @Test
  void testUpdateUpdatedStop() {
    var result = new ModifiedTripBuilder(
      TRIP_TIMES,
      PATTERN,
      SERVICE_DATE,
      timetableRepository.getTimeZone(),
      entityResolver,
      List.of(
        TestCall.of()
          .withStopPointRef(STOP_A_2.getId().getId())
          .withAimedDepartureTime(zonedDateTime(10, 0))
          .withExpectedDepartureTime(zonedDateTime(10, 1))
          .build(),
        TestCall.of()
          .withStopPointRef(STOP_B_1.getId().getId())
          .withAimedArrivalTime(zonedDateTime(10, 10))
          .withExpectedArrivalTime(zonedDateTime(10, 11))
          .withAimedDepartureTime(zonedDateTime(10, 12))
          .withExpectedDepartureTime(zonedDateTime(10, 13))
          .build(),
        TestCall.of()
          .withStopPointRef(STOP_C_1.getId().getId())
          .withAimedArrivalTime(zonedDateTime(10, 20))
          .withExpectedArrivalTime(zonedDateTime(10, 22))
          .build()
      ),
      false,
      null,
      false,
      "DATASOURCE"
    ).build();

    assertTrue(result.isSuccess(), "Update should succeed");

    TripUpdate tripUpdate = result.successValue();
    StopPattern stopPattern = tripUpdate.stopPattern();
    assertNotEquals(PATTERN.getStopPattern(), stopPattern);
    assertEquals(STOP_A_2, stopPattern.getStop(0));
    assertEquals(STOP_B_1, stopPattern.getStop(1));
    assertEquals(STOP_C_1, stopPattern.getStop(2));

    TripTimes updatedTimes = tripUpdate.tripTimes();
    assertEquals(secondsInDay(10, 1), updatedTimes.getArrivalTime(0));
    assertEquals(secondsInDay(10, 1), updatedTimes.getDepartureTime(0));
    assertEquals(secondsInDay(10, 11), updatedTimes.getArrivalTime(1));
    assertEquals(secondsInDay(10, 13), updatedTimes.getDepartureTime(1));
    assertEquals(secondsInDay(10, 22), updatedTimes.getArrivalTime(2));
    assertEquals(secondsInDay(10, 22), updatedTimes.getDepartureTime(2));
    assertEquals(RealTimeState.MODIFIED, updatedTimes.getRealTimeState());
  }

  @Test
  void testCreateStopPatternSameStopCalls() {
    // No change in stops should result in original pattern
    var result = ModifiedTripBuilder.createStopPattern(
      PATTERN,
      List.of(
        TestCall.of().withStopPointRef(STOP_A_1.getId().getId()).build(),
        TestCall.of().withStopPointRef(STOP_B_1.getId().getId()).build(),
        TestCall.of().withStopPointRef(STOP_C_1.getId().getId()).build()
      ),
      entityResolver
    );

    // Assert
    assertTrue(result.isSuccess());
    assertEquals(PATTERN.getStopPattern(), result.successValue());
  }

  @Test
  void testCreateStopPatternSameStationCalls() {
    // Change in stations should result in new pattern
    var result = ModifiedTripBuilder.createStopPattern(
      PATTERN,
      List.of(
        TestCall.of().withStopPointRef(STOP_A_2.getId().getId()).build(),
        TestCall.of().withStopPointRef(STOP_B_1.getId().getId()).build(),
        TestCall.of().withStopPointRef(STOP_C_1.getId().getId()).build()
      ),
      entityResolver
    );

    // Assert
    assertTrue(result.isSuccess());
    assertNotEquals(PATTERN.getStopPattern(), result.successValue());

    var newPattern = PATTERN.copy().withStopPattern(result.successValue()).build();
    assertEquals(STOP_A_2, newPattern.getStop(0));
    assertEquals(STOP_B_1, newPattern.getStop(1));
    assertEquals(STOP_C_1, newPattern.getStop(2));

    for (int i = 0; i < newPattern.numberOfStops(); i++) {
      assertEquals(PickDrop.SCHEDULED, newPattern.getAlightType(i));
      assertEquals(PickDrop.SCHEDULED, newPattern.getBoardType(i));
    }
  }

  @Test
  void testCreateStopPatternDifferentStationCall() {
    // Stop on non-pattern stop, without parent station should be rejected
    var result = ModifiedTripBuilder.createStopPattern(
      PATTERN,
      List.of(
        TestCall.of().withStopPointRef(STOP_A_1.getId().getId()).build(),
        TestCall.of().withStopPointRef(STOP_D.getId().getId()).build(),
        TestCall.of().withStopPointRef(STOP_C_1.getId().getId()).build()
      ),
      entityResolver
    );

    // Assert
    assertTrue(result.isFailure());
    assertEquals(UpdateError.UpdateErrorType.STOP_MISMATCH, result.failureValue().errorType());
  }

  @Test
  void testCreateStopPatternCancelledCall() {
    // Cancellation of a call should be reflected in PickDrop
    var result = ModifiedTripBuilder.createStopPattern(
      PATTERN,
      List.of(
        TestCall.of().withStopPointRef(STOP_A_1.getId().getId()).build(),
        TestCall.of().withStopPointRef(STOP_B_1.getId().getId()).build(),
        TestCall.of().withStopPointRef(STOP_C_1.getId().getId()).withCancellation(true).build()
      ),
      entityResolver
    );

    // Assert
    assertTrue(result.isSuccess());
    assertNotEquals(PATTERN.getStopPattern(), result.successValue());

    var newPattern = PATTERN.copy().withStopPattern(result.successValue()).build();
    assertEquals(STOP_A_1, newPattern.getStop(0));
    assertEquals(STOP_B_1, newPattern.getStop(1));
    assertEquals(STOP_C_1, newPattern.getStop(2));

    assertEquals(PickDrop.SCHEDULED, newPattern.getAlightType(0));
    assertEquals(PickDrop.SCHEDULED, newPattern.getBoardType(0));

    assertEquals(PickDrop.SCHEDULED, newPattern.getAlightType(1));
    assertEquals(PickDrop.SCHEDULED, newPattern.getBoardType(1));

    assertEquals(PickDrop.CANCELLED, newPattern.getAlightType(2));
    assertEquals(PickDrop.CANCELLED, newPattern.getBoardType(2));
  }

  @Test
  void testCreateStopPatternNoBoardingCall() {
    // Cancellation of a call should be reflected in PickDrop
    var result = ModifiedTripBuilder.createStopPattern(
      PATTERN,
      List.of(
        TestCall.of().withStopPointRef(STOP_A_1.getId().getId()).build(),
        TestCall.of()
          .withStopPointRef(STOP_B_1.getId().getId())
          .withDepartureBoardingActivity(DepartureBoardingActivityEnumeration.NO_BOARDING)
          .build(),
        TestCall.of().withStopPointRef(STOP_C_1.getId().getId()).build()
      ),
      entityResolver
    );

    // Assert
    assertTrue(result.isSuccess());
    assertNotEquals(PATTERN.getStopPattern(), result.successValue());

    var newPattern = PATTERN.copy().withStopPattern(result.successValue()).build();
    assertEquals(STOP_A_1, newPattern.getStop(0));
    assertEquals(STOP_B_1, newPattern.getStop(1));
    assertEquals(STOP_C_1, newPattern.getStop(2));

    assertEquals(PickDrop.SCHEDULED, newPattern.getAlightType(0));
    assertEquals(PickDrop.SCHEDULED, newPattern.getBoardType(0));

    assertEquals(PickDrop.SCHEDULED, newPattern.getAlightType(1));
    assertEquals(PickDrop.NONE, newPattern.getBoardType(1));

    assertEquals(PickDrop.SCHEDULED, newPattern.getAlightType(2));
    assertEquals(PickDrop.SCHEDULED, newPattern.getBoardType(2));
  }

  private static ZonedDateTime zonedDateTime(int hour, int minute) {
    return ZonedDateTime.of(SERVICE_DATE, LocalTime.of(hour, minute), TIME_ZONE);
  }

  static int secondsInDay(int hours, int minutes) {
    return (hours * 60 + minutes) * 60;
  }
}
