package org.opentripplanner.ext.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentripplanner.ext.siri.mapper.SiriTransportModeMapper;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.spi.UpdateError;
import uk.org.siri.siri20.VehicleModesEnumeration;

class AddedTripBuilderTest {

  private static final Agency AGENCY = TransitModelForTest.AGENCY;
  private static final ZoneId TIME_ZONE = AGENCY.getTimezone();
  private static final Operator OPERATOR = Operator
    .of(TransitModelForTest.id("OPERATOR_ID"))
    .withName("OPERATOR_NAME")
    .build();
  private static final Route REPLACED_ROUTE = TransitModelForTest
    .route("REPLACED_ROUTE")
    .withAgency(AGENCY)
    .withOperator(OPERATOR)
    .build();
  private static final String LINE_REF = "ROUTE_ID";
  private static final FeedScopedId TRIP_ID = TransitModelForTest.id("TRIP_ID");
  private static final LocalDate SERVICE_DATE = LocalDate.of(2023, 2, 17);
  private static final TransitMode TRANSIT_MODE = TransitMode.RAIL;
  private static final String SUB_MODE = "replacementRailService";
  private static final String SHORT_NAME = "Hogwarts Express";
  private static final String HEADSIGN = "TEST TRIP TOWARDS TEST ISLAND";

  /* Transit model */
  private static final TransitModelForTest MODEL_TEST = TransitModelForTest.of();

  private static final RegularStop STOP_A = MODEL_TEST.stop("A").build();
  private static final RegularStop STOP_B = MODEL_TEST.stop("B").build();
  private static final RegularStop STOP_C = MODEL_TEST.stop("C").build();
  private static final RegularStop STOP_D = MODEL_TEST.stop("D").build();
  private final StopModel STOP_MODEL = MODEL_TEST
    .stopModelBuilder()
    .withRegularStop(STOP_A)
    .withRegularStop(STOP_B)
    .withRegularStop(STOP_C)
    .withRegularStop(STOP_D)
    .build();

  private final Deduplicator DEDUPLICATOR = new Deduplicator();
  private final TransitModel TRANSIT_MODEL = new TransitModel(STOP_MODEL, DEDUPLICATOR);
  private EntityResolver ENTITY_RESOLVER;

  @BeforeEach
  void setUp() {
    // Add entities to transit model for the entity resolver
    TRANSIT_MODEL.addAgency(AGENCY);
    final TripPattern pattern = TransitModelForTest
      .tripPattern("REPLACED_ROUTE_PATTERN_ID", REPLACED_ROUTE)
      .withStopPattern(TransitModelForTest.stopPattern(STOP_A, STOP_B))
      .build();
    TRANSIT_MODEL.addTripPattern(pattern.getId(), pattern);

    // Crate a scheduled calendar, to have the SERVICE_DATE be within the transit feed coverage
    CalendarServiceData calendarServiceData = new CalendarServiceData();
    var cal_id = TransitModelForTest.id("CAL_1");
    calendarServiceData.putServiceDatesForServiceId(
      cal_id,
      List.of(SERVICE_DATE.minusDays(1), SERVICE_DATE, SERVICE_DATE.plusDays(1))
    );
    TRANSIT_MODEL.getServiceCodes().put(cal_id, 0);
    TRANSIT_MODEL.updateCalendarServiceData(true, calendarServiceData, DataImportIssueStore.NOOP);

    // Create transit model index
    TRANSIT_MODEL.index();

    // Create the entity resolver only after the model has been indexed
    ENTITY_RESOLVER =
      new EntityResolver(new DefaultTransitService(TRANSIT_MODEL), TransitModelForTest.FEED_ID);
  }

  @Test
  void testAddedTrip() {
    var addedTrip = new AddedTripBuilder(
      TRANSIT_MODEL,
      ENTITY_RESOLVER,
      AbstractTransitEntity::getId,
      TRIP_ID,
      OPERATOR,
      LINE_REF,
      REPLACED_ROUTE,
      SERVICE_DATE,
      TRANSIT_MODE,
      SUB_MODE,
      getCalls(10),
      false,
      null,
      false,
      SHORT_NAME,
      HEADSIGN,
      List.of()
    )
      .build();

    assertTrue(addedTrip.isSuccess(), "Trip creation should succeed");

    // Assert trip
    Trip trip = addedTrip.successValue().tripTimes().getTrip();
    assertEquals(TRIP_ID, trip.getId(), "Trip should be mapped");
    assertEquals(OPERATOR, trip.getOperator(), "operator should be mapped");
    assertEquals(TRANSIT_MODE, trip.getMode(), "transitMode should be mapped");
    assertEquals(SubMode.of(SUB_MODE), trip.getNetexSubMode(), "submode should be mapped");
    assertNotNull(trip.getHeadsign(), "Headsign should be mapped");
    assertEquals(HEADSIGN, trip.getHeadsign().toString(), "Headsign should be mapped");
    assertEquals(SERVICE_DATE, addedTrip.successValue().serviceDate());

    // Assert route
    Route route = trip.getRoute();
    assertEquals(LINE_REF, route.getId().getId(), "route should be mapped");
    assertEquals(AGENCY, route.getAgency(), "Agency should be taken from replaced route");
    assertEquals(SHORT_NAME, route.getShortName());
    assertEquals(TRANSIT_MODE, route.getMode(), "transitMode should be mapped");
    assertEquals(SubMode.of(SUB_MODE), route.getNetexSubmode(), "submode should be mapped");
    assertNotEquals(REPLACED_ROUTE, route, "Should not re-use replaced route");

    // Assert transit model index
    var transitModelIndex = TRANSIT_MODEL.getTransitModelIndex();
    assertNotNull(transitModelIndex);
    assertEquals(
      route,
      transitModelIndex.getRouteForId(TransitModelForTest.id(LINE_REF)),
      "Route should be added to transit index"
    );
    assertEquals(
      trip,
      transitModelIndex.getTripForId().get(TRIP_ID),
      "Route should be added to transit index"
    );
    var pattern = transitModelIndex.getPatternForTrip().get(trip);
    assertNotNull(pattern);
    assertEquals(route, pattern.getRoute());
    assertTrue(
      transitModelIndex
        .getServiceCodesRunningForDate()
        .get(SERVICE_DATE)
        .contains(TRANSIT_MODEL.getServiceCodes().get(trip.getServiceId())),
      "serviceId should be running on service date"
    );
    assertNotNull(
      transitModelIndex.getTripOnServiceDateById().get(TRIP_ID),
      "TripOnServiceDate should be added to transit index by id"
    );
    assertNotNull(
      transitModelIndex
        .getTripOnServiceDateForTripAndDay()
        .get(new TripIdAndServiceDate(TRIP_ID, SERVICE_DATE)),
      "TripOnServiceDate should be added to transit index for trip and day"
    );

    // Assert stop pattern
    var stopPattern = addedTrip.successValue().stopPattern();
    assertEquals(stopPattern, pattern.getStopPattern());
    assertEquals(3, stopPattern.getSize());
    assertEquals(STOP_A, stopPattern.getStop(0));
    assertEquals(STOP_B, stopPattern.getStop(1));
    assertEquals(STOP_C, stopPattern.getStop(2));

    // Assert scheduled timetable
    var scheduledTimes = pattern.getScheduledTimetable().getTripTimes(trip);
    assertNotNull(scheduledTimes);
    // TODO - is this correct?
    assertEquals(RealTimeState.SCHEDULED, scheduledTimes.getRealTimeState());
    assertTrue(scheduledTimes.isScheduled());
    assertEquals(secondsInDay(10, 20), scheduledTimes.getArrivalTime(0));
    assertEquals(secondsInDay(10, 20), scheduledTimes.getDepartureTime(0));
    assertEquals(0, scheduledTimes.getDepartureDelay(0));
    assertEquals(HEADSIGN, scheduledTimes.getHeadsign(0).toString());
    assertFalse(
      scheduledTimes.isRecordedStop(0),
      "Scheduled timetable should not have actual departure time"
    );
    assertEquals(secondsInDay(10, 30), scheduledTimes.getArrivalTime(1));
    assertEquals(secondsInDay(10, 30), scheduledTimes.getDepartureTime(1));
    assertEquals(0, scheduledTimes.getArrivalDelay(1));
    assertEquals(0, scheduledTimes.getDepartureDelay(1));
    assertEquals(secondsInDay(10, 40), scheduledTimes.getArrivalTime(2));
    assertEquals(secondsInDay(10, 40), scheduledTimes.getDepartureTime(2));
    assertEquals(0, scheduledTimes.getArrivalDelay(2));

    // Assert updated trip times
    var times = addedTrip.successValue().tripTimes();
    assertEquals(trip, times.getTrip());
    assertEquals(RealTimeState.ADDED, times.getRealTimeState());
    assertFalse(times.isScheduled());
    assertEquals(secondsInDay(10, 19), times.getArrivalTime(0));
    assertEquals(secondsInDay(10, 19), times.getDepartureTime(0));
    assertEquals(-60, times.getDepartureDelay(0));
    assertEquals(HEADSIGN, times.getHeadsign(0).toString());
    assertTrue(times.isRecordedStop(0), "First stop has actual departure time");
    assertEquals(secondsInDay(10, 29), times.getArrivalTime(1));
    assertEquals(secondsInDay(10, 31), times.getDepartureTime(1));
    assertEquals(-60, times.getArrivalDelay(1));
    assertEquals(60, times.getDepartureDelay(1));
    assertFalse(times.isRecordedStop(1), "First stop has actual departure time");
    assertEquals(secondsInDay(10, 41), times.getArrivalTime(2));
    assertEquals(secondsInDay(10, 41), times.getDepartureTime(2));
    assertEquals(60, times.getArrivalDelay(2));
    assertFalse(times.isRecordedStop(2), "First stop has actual departure time");
  }

  @Test
  void testAddedTripOnAddedRoute() {
    var firstAddedTrip = new AddedTripBuilder(
      TRANSIT_MODEL,
      ENTITY_RESOLVER,
      AbstractTransitEntity::getId,
      TRIP_ID,
      OPERATOR,
      LINE_REF,
      REPLACED_ROUTE,
      SERVICE_DATE,
      TRANSIT_MODE,
      SUB_MODE,
      getCalls(10),
      false,
      null,
      false,
      SHORT_NAME,
      HEADSIGN,
      List.of()
    )
      .build();

    assertTrue(firstAddedTrip.isSuccess(), "Trip creation should succeed");
    var firstTrip = firstAddedTrip.successValue().tripTimes().getTrip();

    var tripId2 = TransitModelForTest.id("TRIP_ID_2");

    var secondAddedTrip = new AddedTripBuilder(
      TRANSIT_MODEL,
      ENTITY_RESOLVER,
      AbstractTransitEntity::getId,
      tripId2,
      OPERATOR,
      LINE_REF,
      REPLACED_ROUTE,
      SERVICE_DATE,
      TRANSIT_MODE,
      SUB_MODE,
      getCalls(11),
      false,
      null,
      false,
      SHORT_NAME,
      HEADSIGN,
      List.of()
    )
      .build();

    assertTrue(secondAddedTrip.isSuccess(), "Trip creation should succeed");

    // Assert trip
    Trip secondTrip = secondAddedTrip.successValue().tripTimes().getTrip();
    assertEquals(tripId2, secondTrip.getId(), "Trip should be mapped");
    assertNotEquals(firstTrip, secondTrip);

    // Assert route
    Route route = secondTrip.getRoute();
    assertSame(firstTrip.getRoute(), route, "route be reused from the first trip");

    // Assert transit model index
    var transitModelIndex = TRANSIT_MODEL.getTransitModelIndex();
    assertNotNull(transitModelIndex);
    assertEquals(2, transitModelIndex.getPatternsForRoute().get(route).size());

    // Assert trip times
    var times = secondAddedTrip.successValue().tripTimes();
    assertEquals(secondTrip, times.getTrip());
    assertEquals(RealTimeState.ADDED, times.getRealTimeState());
    assertEquals(secondsInDay(11, 19), times.getArrivalTime(0));
    assertEquals(secondsInDay(11, 19), times.getDepartureTime(0));
    assertEquals(secondsInDay(11, 29), times.getArrivalTime(1));
    assertEquals(secondsInDay(11, 31), times.getDepartureTime(1));
    assertEquals(secondsInDay(11, 41), times.getArrivalTime(2));
    assertEquals(secondsInDay(11, 41), times.getDepartureTime(2));
  }

  @Test
  void testAddedTripOnExistingRoute() {
    var addedTrip = new AddedTripBuilder(
      TRANSIT_MODEL,
      ENTITY_RESOLVER,
      AbstractTransitEntity::getId,
      TRIP_ID,
      OPERATOR,
      REPLACED_ROUTE.getId().getId(),
      REPLACED_ROUTE,
      SERVICE_DATE,
      TRANSIT_MODE,
      SUB_MODE,
      getCalls(10),
      false,
      null,
      false,
      SHORT_NAME,
      HEADSIGN,
      List.of()
    )
      .build();

    assertTrue(addedTrip.isSuccess(), "Trip creation should succeed");

    // Assert trip
    Trip trip = addedTrip.successValue().tripTimes().getTrip();
    assertEquals(TRIP_ID, trip.getId(), "Trip should be mapped");
    assertSame(REPLACED_ROUTE, trip.getRoute());
  }

  @Test
  void testAddedTripWithoutReplacedRoute() {
    var addedTrip = new AddedTripBuilder(
      TRANSIT_MODEL,
      ENTITY_RESOLVER,
      AbstractTransitEntity::getId,
      TRIP_ID,
      OPERATOR,
      LINE_REF,
      null,
      SERVICE_DATE,
      TRANSIT_MODE,
      null,
      getCalls(10),
      false,
      null,
      false,
      SHORT_NAME,
      HEADSIGN,
      List.of()
    )
      .build();

    assertTrue(addedTrip.isSuccess(), "Trip creation should succeed");

    // Assert trip
    Trip trip = addedTrip.successValue().tripTimes().getTrip();
    assertEquals(TRIP_ID, trip.getId(), "Trip should be mapped");

    // Assert route
    Route route = trip.getRoute();
    assertEquals(LINE_REF, route.getId().getId(), "route should be mapped");
    assertEquals(AGENCY, route.getAgency(), "Agency should be taken from replaced route");
    assertEquals(SHORT_NAME, route.getShortName());
    assertEquals(TRANSIT_MODE, route.getMode(), "transitMode should be mapped");
    assertEquals(
      SubMode.UNKNOWN,
      route.getNetexSubmode(),
      "submode should be unknown, when ro replacing route is found"
    );
    assertNotEquals(REPLACED_ROUTE, route, "Should not re-use replaced route");
  }

  @Test
  void testAddedTripFailOnMissingServiceId() {
    var addedTrip = new AddedTripBuilder(
      TRANSIT_MODEL,
      ENTITY_RESOLVER,
      AbstractTransitEntity::getId,
      TRIP_ID,
      OPERATOR,
      LINE_REF,
      REPLACED_ROUTE,
      null,
      TRANSIT_MODE,
      SUB_MODE,
      getCalls(0),
      false,
      null,
      false,
      SHORT_NAME,
      HEADSIGN,
      List.of()
    )
      .build();

    assertTrue(addedTrip.isFailure(), "Trip creation should fail");
    assertEquals(
      UpdateError.UpdateErrorType.NO_START_DATE,
      addedTrip.failureValue().errorType(),
      "Trip creation should fail without start date"
    );
  }

  @Test
  void testAddedTripFailOnNonIncreasingDwellTime() {
    List<CallWrapper> calls = List.of(
      TestCall
        .of()
        .withStopPointRef(STOP_A.getId().getId())
        .withAimedDepartureTime(zonedDateTime(10, 20))
        .withExpectedDepartureTime(zonedDateTime(10, 20))
        .build(),
      TestCall
        .of()
        .withStopPointRef(STOP_B.getId().getId())
        .withAimedArrivalTime(zonedDateTime(10, 30))
        .withExpectedArrivalTime(zonedDateTime(10, 31))
        .withAimedDepartureTime(zonedDateTime(10, 30))
        .withExpectedDepartureTime(zonedDateTime(10, 29))
        .build(),
      // Expected to arrive one minute prior to irrelevant aimed departure time
      TestCall
        .of()
        .withStopPointRef(STOP_C.getId().getId())
        .withAimedArrivalTime(zonedDateTime(10, 40))
        .withExpectedArrivalTime(zonedDateTime(10, 40))
        .build()
    );

    var addedTrip = new AddedTripBuilder(
      TRANSIT_MODEL,
      ENTITY_RESOLVER,
      AbstractTransitEntity::getId,
      TRIP_ID,
      OPERATOR,
      LINE_REF,
      REPLACED_ROUTE,
      SERVICE_DATE,
      TRANSIT_MODE,
      SUB_MODE,
      calls,
      false,
      null,
      false,
      SHORT_NAME,
      HEADSIGN,
      List.of()
    )
      .build();

    assertTrue(addedTrip.isFailure(), "Trip creation should fail");
    assertEquals(
      UpdateError.UpdateErrorType.NEGATIVE_DWELL_TIME,
      addedTrip.failureValue().errorType(),
      "Trip creation should fail with invalid dwell time"
    );
  }

  @Test
  void testAddedTripFailOnTooFewCalls() {
    List<CallWrapper> calls = List.of(
      TestCall
        .of()
        .withStopPointRef(STOP_A.getId().getId())
        .withAimedDepartureTime(zonedDateTime(10, 20))
        .withExpectedDepartureTime(zonedDateTime(10, 20))
        .build()
    );
    var addedTrip = new AddedTripBuilder(
      TRANSIT_MODEL,
      ENTITY_RESOLVER,
      AbstractTransitEntity::getId,
      TRIP_ID,
      OPERATOR,
      LINE_REF,
      REPLACED_ROUTE,
      SERVICE_DATE,
      TRANSIT_MODE,
      SUB_MODE,
      calls,
      false,
      null,
      false,
      SHORT_NAME,
      HEADSIGN,
      List.of()
    )
      .build();

    assertTrue(addedTrip.isFailure(), "Trip creation should fail");
    assertEquals(
      UpdateError.UpdateErrorType.TOO_FEW_STOPS,
      addedTrip.failureValue().errorType(),
      "Trip creation should fail with too few calls"
    );
  }

  @Test
  void testAddedTripFailOnUnknownStop() {
    List<CallWrapper> calls = List.of(
      TestCall
        .of()
        .withStopPointRef("UNKNOWN_STOP_REF")
        .withAimedDepartureTime(zonedDateTime(10, 20))
        .withExpectedDepartureTime(zonedDateTime(10, 20))
        .build(),
      TestCall
        .of()
        .withStopPointRef(STOP_B.getId().getId())
        .withAimedArrivalTime(zonedDateTime(10, 30))
        .withExpectedArrivalTime(zonedDateTime(10, 31))
        .withAimedDepartureTime(zonedDateTime(10, 30))
        .withExpectedDepartureTime(zonedDateTime(10, 29))
        .build()
    );
    var addedTrip = new AddedTripBuilder(
      TRANSIT_MODEL,
      ENTITY_RESOLVER,
      AbstractTransitEntity::getId,
      TRIP_ID,
      OPERATOR,
      LINE_REF,
      REPLACED_ROUTE,
      SERVICE_DATE,
      TRANSIT_MODE,
      SUB_MODE,
      calls,
      false,
      null,
      false,
      SHORT_NAME,
      HEADSIGN,
      List.of()
    )
      .build();

    assertTrue(addedTrip.isFailure(), "Trip creation should fail");
    assertEquals(
      UpdateError.UpdateErrorType.NO_VALID_STOPS,
      addedTrip.failureValue().errorType(),
      "Trip creation should fail with call referring to unknown stop"
    );
  }

  @ParameterizedTest
  @CsvSource(
    {
      "air,AIRPLANE,AIRPLANE,",
      "bus,BUS,RAIL,railReplacementBus",
      "rail,RAIL,RAIL,replacementRailService",
      "ferry,FERRY,RAIL,",
    }
  )
  void testGetTransportMode(
    String siriMode,
    String internalMode,
    String replacedRouteMode,
    String subMode
  ) {
    // Arrange
    var route = Route
      .of(TransitModelForTest.id(LINE_REF))
      .withShortName(SHORT_NAME)
      .withAgency(AGENCY)
      .withMode(TransitMode.valueOf(replacedRouteMode))
      .build();
    var modes = List.of(VehicleModesEnumeration.fromValue(siriMode));

    // Act
    TransitMode transitMode = SiriTransportModeMapper.mapTransitMainMode(modes);
    String transitSubMode = AddedTripBuilder.resolveTransitSubMode(transitMode, route);

    //Assert
    var expectedMode = TransitMode.valueOf(internalMode);
    assertEquals(expectedMode, transitMode, "Mode not mapped to correct internal mode");
    assertEquals(subMode, transitSubMode, "Mode not mapped to correct sub mode");
  }

  private static List<CallWrapper> getCalls(int hour) {
    return List.of(
      // Departed one minute early, prior to irrelevant aimed arrival time
      TestCall
        .of()
        .withStopPointRef(STOP_A.getId().getId())
        .withAimedDepartureTime(zonedDateTime(hour, 20))
        .withExpectedDepartureTime(zonedDateTime(hour, 20))
        .withActualDepartureTime(zonedDateTime(hour, 19))
        .build(),
      TestCall
        .of()
        .withStopPointRef(STOP_B.getId().getId())
        .withAimedArrivalTime(zonedDateTime(hour, 30))
        .withExpectedArrivalTime(zonedDateTime(hour, 29))
        .withAimedDepartureTime(zonedDateTime(hour, 30))
        .withExpectedDepartureTime(zonedDateTime(hour, 31))
        .build(),
      // Expected to arrive one minute prior to irrelevant aimed departure time
      TestCall
        .of()
        .withStopPointRef(STOP_C.getId().getId())
        .withAimedArrivalTime(zonedDateTime(hour, 40))
        .withExpectedArrivalTime(zonedDateTime(hour, 41))
        .build()
    );
  }

  private static ZonedDateTime zonedDateTime(int hour, int minute) {
    return ZonedDateTime.of(SERVICE_DATE, LocalTime.of(hour, minute), TIME_ZONE);
  }

  private int secondsInDay(int hour, int minute) {
    return (hour * 60 + minute) * 60;
  }
}
