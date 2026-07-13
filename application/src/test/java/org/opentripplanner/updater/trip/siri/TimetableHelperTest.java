package org.opentripplanner.updater.trip.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.SiteRepository;
import uk.org.siri.siri21.CallStatusEnumeration;
import uk.org.siri.siri21.OccupancyEnumeration;

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

  private RealTimeTripTimesBuilder builder;

  @BeforeEach
  public void setUp() {
    Station station = Station.of(SCOPED_STATION_ID)
      .withName(new NonLocalizedString(STATION_NAME))
      .withCoordinate(0.0, 0.0)
      .build();

    var stopTime = new StopTime();
    stopTime.setArrivalTime(3600);
    stopTime.setDepartureTime(3660);
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
    builder = TripTimesFactory.tripTimes(
      trip,
      List.of(stopTime),
      new Deduplicator()
    ).createRealTimeFromScheduledTimes();
  }

  @Test
  public void testApplyUpdates_MapPredictionInaccurate_EstimatedCall() {
    CallWrapper estimatedCall = TestCall.of()
      .withStopPointRef(STOP_ID)
      .withCancellation(false)
      .withOccupancy(OccupancyEnumeration.SEATS_AVAILABLE)
      .withPredictionInaccurate(true)
      .build();

    TimetableHelper.applyUpdates(START_OF_SERVICE, builder, 0, false, false, estimatedCall, null);

    assertEquals(
      "Occupancy:MANY_SEATS_AVAILABLE Cancelled:false Extra:false Arrived:false Departed:false Inaccurate:true",
      showStatuses(0)
    );
  }

  @Test
  public void testApplyUpdates_CancellationPriorityOverPredictionInaccurate_EstimatedCall() {
    CallWrapper estimatedCall = TestCall.of()
      .withStopPointRef(STOP_ID)
      .withCancellation(true)
      .withOccupancy(OccupancyEnumeration.FULL)
      .withPredictionInaccurate(true)
      .build();

    TimetableHelper.applyUpdates(START_OF_SERVICE, builder, 0, false, false, estimatedCall, null);

    assertEquals(
      "Occupancy:FULL Cancelled:true Extra:false Arrived:false Departed:false Inaccurate:false",
      showStatuses(0)
    );
  }

  @Test
  public void testApplyUpdates_CancellationPriorityOverPredictionInaccurate_RecordedCall() {
    ZonedDateTime actualTime = START_OF_SERVICE.plus(Duration.ofHours(1));
    CallWrapper recordedCall = TestCall.of()
      .withStopPointRef(STOP_ID)
      .withPredictionInaccurate(true)
      .withOccupancy(OccupancyEnumeration.FULL)
      .withCancellation(true)
      .withActualDepartureTime(actualTime)
      .build();

    TimetableHelper.applyUpdates(START_OF_SERVICE, builder, 0, false, false, recordedCall, null);

    assertEquals(
      "Occupancy:FULL Cancelled:true Extra:false Arrived:false Departed:false Inaccurate:false",
      showStatuses(0)
    );
  }

  @Test
  public void testApplyUpdates_PredictionInaccuratePriorityOverRecorded() {
    CallWrapper recordedCall = TestCall.of()
      .withStopPointRef(STOP_ID)
      .withPredictionInaccurate(true)
      .withOccupancy(OccupancyEnumeration.FULL)
      .withCancellation(false)
      .withActualDepartureTime(START_OF_SERVICE.plus(Duration.ofHours(1)))
      .build();

    TimetableHelper.applyUpdates(START_OF_SERVICE, builder, 0, false, false, recordedCall, null);

    assertEquals(
      "Occupancy:FULL Cancelled:false Extra:false Arrived:false Departed:false Inaccurate:true",
      showStatuses(0)
    );
  }

  @Test
  public void testApplyUpdates_RecordedCallResultsInArrivedAndDeparted() {
    CallWrapper recordedCall = TestCall.of()
      .withStopPointRef(STOP_ID)
      .withPredictionInaccurate(false)
      .withOccupancy(OccupancyEnumeration.STANDING_AVAILABLE)
      .withCancellation(false)
      .withActualDepartureTime(START_OF_SERVICE.plus(Duration.ofHours(1)))
      .withIsRecorded(true)
      .build();

    TimetableHelper.applyUpdates(START_OF_SERVICE, builder, 0, false, false, recordedCall, null);

    assertEquals(
      "Occupancy:STANDING_ROOM_ONLY Cancelled:false Extra:false Arrived:true Departed:true Inaccurate:false",
      showStatuses(0)
    );
  }

  @Test
  public void testApplyUpdates_Arrived() {
    CallWrapper recordedCall = TestCall.of()
      .withStopPointRef(STOP_ID)
      .withPredictionInaccurate(false)
      .withOccupancy(OccupancyEnumeration.STANDING_AVAILABLE)
      .withCancellation(false)
      .withActualDepartureTime(START_OF_SERVICE.plus(Duration.ofHours(1)))
      .withArrivalStatus(CallStatusEnumeration.ARRIVED)
      .build();

    TimetableHelper.applyUpdates(START_OF_SERVICE, builder, 0, false, false, recordedCall, null);

    assertEquals(
      "Occupancy:STANDING_ROOM_ONLY Cancelled:false Extra:false Arrived:true Departed:false Inaccurate:false",
      showStatuses(0)
    );
  }

  @Test
  public void testApplyUpdates_JourneyDefaultValues() {
    CallWrapper recordedCall = TestCall.of().withStopPointRef(STOP_ID).build();

    TimetableHelper.applyUpdates(
      START_OF_SERVICE,
      builder,
      0,
      false,
      true,
      recordedCall,
      OccupancyStatus.STANDING_ROOM_ONLY
    );

    assertEquals(
      "Occupancy:STANDING_ROOM_ONLY Cancelled:false Extra:false Arrived:false Departed:false Inaccurate:true",
      showStatuses(0)
    );
  }

  @Test
  public void testApplyUpdates_RecordedCallArrivedButNotDeparted() {
    ZonedDateTime actualArrival = START_OF_SERVICE.plus(Duration.ofHours(1));
    ZonedDateTime expectedDeparture = actualArrival.plus(Duration.ofMinutes(5));
    CallWrapper recordedCall = TestCall.of()
      .withStopPointRef(STOP_ID)
      .withPredictionInaccurate(false)
      .withCancellation(false)
      .withActualArrivalTime(actualArrival)
      .withExpectedDepartureTime(expectedDeparture)
      .withIsRecorded(true)
      .build();

    TimetableHelper.applyUpdates(START_OF_SERVICE, builder, 0, false, false, recordedCall, null);

    assertEquals(
      "Occupancy:NO_DATA_AVAILABLE Cancelled:false Extra:false Arrived:true Departed:false Inaccurate:false",
      showStatuses(0)
    );
  }

  @Test
  public void testApplyUpdates_ExtraCall_EstimatedCall() {
    CallWrapper estimatedCall = TestCall.of().withExtraCall(true).build();

    TimetableHelper.applyUpdates(START_OF_SERVICE, builder, 0, false, false, estimatedCall, null);

    assertEquals(
      "Occupancy:NO_DATA_AVAILABLE Cancelled:false Extra:true Arrived:false Departed:false Inaccurate:false",
      showStatuses(0)
    );
  }

  @Test
  public void testApplyUpdates_NoRealtime_setsNoDataOnStop() {
    // A call with no realtime times at all should mark the stop as NO_DATA
    // and must NOT set timesModified (so hasAnyUpdates stays false).
    CallWrapper callWithNoTimes = TestCall.of().withStopPointRef(STOP_ID).build();

    TimetableHelper.applyUpdates(START_OF_SERVICE, builder, 0, false, false, callWithNoTimes, null);

    RealTimeTripTimes tripTimes = builder.build();
    assertTrue(
      tripTimes.isNoDataStop(0),
      "Stop should be marked NO_DATA when no realtime times provided"
    );
    assertFalse(
      tripTimes.isTimesModified(),
      "timesModified must remain false when only NO_DATA stops"
    );
    assertFalse(
      tripTimes.hasAnyUpdates(),
      "hasAnyUpdates must be false when no times were modified"
    );
  }

  @Test
  public void testApplyUpdates_OnlyExpectedDeparture_doesNotSetNoData() {
    // A call with only a departure time (no arrival) must still be treated as a real-time update
    // and must NOT mark the stop as NO_DATA.
    ZonedDateTime expectedDeparture = START_OF_SERVICE.plus(Duration.ofHours(1));
    CallWrapper callWithOnlyDeparture = TestCall.of()
      .withStopPointRef(STOP_ID)
      .withExpectedDepartureTime(expectedDeparture)
      .build();

    TimetableHelper.applyUpdates(
      START_OF_SERVICE,
      builder,
      0,
      false,
      false,
      callWithOnlyDeparture,
      null
    );

    RealTimeTripTimes tripTimes = builder.build();
    assertFalse(
      tripTimes.isNoDataStop(0),
      "Stop must NOT be NO_DATA when a departure time is provided"
    );
    assertTrue(
      tripTimes.isTimesModified(),
      "timesModified must be true when a realtime departure is applied"
    );
  }

  @Test
  public void testApplyUpdates_OnlyExpectedArrival_doesNotSetNoData() {
    // A call with only an arrival time (no departure) must still be treated as a real-time update.
    // The arrival must be ≤ scheduled departure (3660s) to avoid a negative dwell time.
    ZonedDateTime expectedArrival = START_OF_SERVICE.plus(Duration.ofSeconds(3620));
    CallWrapper callWithOnlyArrival = TestCall.of()
      .withStopPointRef(STOP_ID)
      .withExpectedArrivalTime(expectedArrival)
      .build();

    TimetableHelper.applyUpdates(
      START_OF_SERVICE,
      builder,
      0,
      false,
      false,
      callWithOnlyArrival,
      null
    );

    RealTimeTripTimes tripTimes = builder.build();
    assertFalse(
      tripTimes.isNoDataStop(0),
      "Stop must NOT be NO_DATA when an arrival time is provided"
    );
    assertTrue(
      tripTimes.isTimesModified(),
      "timesModified must be true when a realtime arrival is applied"
    );
  }

  @Test
  public void testApplyUpdates_NoDataStop_doesNotPreventCancellationFromBeingApplied() {
    // A cancelled stop with no realtime times: cancellation must still be set
    // even though the times are NO_DATA.
    CallWrapper cancelledNoTimesCall = TestCall.of()
      .withStopPointRef(STOP_ID)
      .withCancellation(true)
      .build();

    TimetableHelper.applyUpdates(
      START_OF_SERVICE,
      builder,
      0,
      false,
      false,
      cancelledNoTimesCall,
      null
    );

    assertEquals(
      "Occupancy:NO_DATA_AVAILABLE Cancelled:true Extra:false Arrived:false Departed:false Inaccurate:false",
      showStatuses(0)
    );
  }

  private String showStatuses(int index) {
    RealTimeTripTimes tripTimes = builder.build();
    return (
      "Occupancy:" +
      tripTimes.getOccupancyStatus(index) +
      " Cancelled:" +
      tripTimes.isCanceledStop(index) +
      " Extra:" +
      tripTimes.isExtraCall(index) +
      " Arrived:" +
      tripTimes.hasArrived(index) +
      " Departed:" +
      tripTimes.hasDeparted(index) +
      " Inaccurate:" +
      tripTimes.isPredictionInaccurate(index)
    );
  }
}
