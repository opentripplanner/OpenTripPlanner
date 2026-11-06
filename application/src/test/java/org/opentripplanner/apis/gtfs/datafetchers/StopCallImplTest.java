package org.opentripplanner.apis.gtfs.datafetchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentripplanner.apis.gtfs.model.CallSchedule;
import org.opentripplanner.apis.gtfs.model.CallScheduledTime.ArrivalDepartureTime;
import org.opentripplanner.apis.gtfs.model.CallScheduledTime.TimeWindow;
import org.opentripplanner.apis.support.graphql.DataFetchingSupport;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.TripAlteration;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.ServiceDateUtils;

class StopCallImplTest {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2023, 6, 3);
  private static final ZoneId TIME_ZONE = ZoneId.of("Europe/Paris");
  private static final Instant MIDNIGHT = ServiceDateUtils.asStartOfService(
    SERVICE_DATE,
    TIME_ZONE
  ).toInstant();
  private static final String TRIP_ID = "Trip1";
  private static final String FLEX_TRIP_ID = "FlexTrip1";

  private static final OffsetDateTime NOON = OffsetDateTime.parse("2023-06-03T12:00+02:00");
  private static final OffsetDateTime TEN_AM = NOON.minusHours(2);
  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of(SERVICE_DATE);
  private final RegularStop STOP_A = envBuilder.stop("A");
  private final RegularStop STOP_B = envBuilder.stop("B");
  private final RegularStop STOP_C = envBuilder.stop("C");
  private final AreaStop STOP_D = envBuilder.areaStop("D");
  private final AreaStop STOP_E = envBuilder.areaStop("E");

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_ID)
    .addStop(STOP_A, "12:00:00", "12:00:00")
    .addStop(STOP_B, "12:30:00", "12:30:00")
    .addStop(STOP_C, "13:00:00", "13:00:00");

  private final TripInput FLEX_TRIP_INPUT = TripInput.flex(FLEX_TRIP_ID)
    .addStop(STOP_D, "10:00", "10:30")
    .addStop(STOP_E, "11:00", "11:30");

  @Test
  void fixedTrip() throws Exception {
    var realtimeEnv = envBuilder.addTrip(TRIP_INPUT).build();
    var tripData = realtimeEnv.tripData(TRIP_ID);
    var tripTimes = tripData.tripTimes();
    var pattern = tripData.tripPattern();

    var call = new TripTimeOnDate(tripTimes, 0, pattern, SERVICE_DATE, MIDNIGHT);

    var impl = new StopCallImpl();
    var env = DataFetchingSupport.dataFetchingEnvironment(
      call,
      Map.of(),
      realtimeEnv.transitService()
    );

    var schedule = impl.schedule().get(env);
    assertEquals(new CallSchedule(new ArrivalDepartureTime(NOON, NOON, false), false), schedule);
  }

  @Test
  void canceledStop() throws Exception {
    var realtimeEnv = envBuilder.addTrip(TRIP_INPUT).build();
    var tripData = realtimeEnv.tripData(TRIP_ID);
    var pattern = tripData.tripPattern();
    var tripTimes = tripData
      .tripTimes()
      .createRealTimeFromScheduledTimes()
      .withRealTimeUpdated()
      .withCanceled(2)
      .build();

    for (int stopPos = 0; stopPos < 3; stopPos++) {
      var call = new TripTimeOnDate(tripTimes, stopPos, pattern, SERVICE_DATE, MIDNIGHT);
      assertCancellation(false, stopPos == 2, call, realtimeEnv.transitService());
    }
  }

  @Test
  void canceledTripCancelsAllCalls() throws Exception {
    var realtimeEnv = envBuilder.addTrip(TRIP_INPUT).build();
    var tripData = realtimeEnv.tripData(TRIP_ID);
    var pattern = tripData.tripPattern();
    var tripTimes = tripData.tripTimes().createRealTimeFromScheduledTimes().withCanceled().build();

    for (int stopPos = 0; stopPos < 3; stopPos++) {
      var call = new TripTimeOnDate(tripTimes, stopPos, pattern, SERVICE_DATE, MIDNIGHT);
      assertCancellation(false, true, call, realtimeEnv.transitService());
    }
  }

  /**
   * A real-time pattern can cancel a stop by setting its pickup and drop-off to cancelled, without
   * the trip times marking the stop as canceled. The cancellation is reported in {@code realTime},
   * even if the trip times are not marked as updated.
   */
  @Test
  void stopCanceledByPattern() throws Exception {
    var realtimeEnv = envBuilder.addTrip(TRIP_INPUT).build();
    var tripData = realtimeEnv.tripData(TRIP_ID);
    var scheduledPattern = tripData.tripPattern();
    var stopPattern = StopPattern.create(3);
    var stops = List.of(STOP_A, STOP_B, STOP_C);
    for (int i = 0; i < 3; i++) {
      var pickDrop = i == 1 ? PickDrop.CANCELLED : PickDrop.SCHEDULED;
      stopPattern.stops.with(i, stops.get(i));
      stopPattern.pickups.with(i, pickDrop);
      stopPattern.dropoffs.with(i, pickDrop);
    }
    var pattern = TripPattern.of(id("PatternWithCanceledStop"))
      .withRoute(scheduledPattern.getRoute())
      .withMode(scheduledPattern.getMode())
      .withStopPattern(stopPattern.build())
      .build();

    for (int stopPos = 0; stopPos < 3; stopPos++) {
      var call = new TripTimeOnDate(tripData.tripTimes(), stopPos, pattern, SERVICE_DATE, MIDNIGHT);
      assertCancellation(false, stopPos == 1, call, realtimeEnv.transitService());
    }
  }

  @ParameterizedTest
  @EnumSource(value = TripAlteration.class, names = { "CANCELLATION", "REPLACED" })
  void tripCanceledInPlannedData(TripAlteration alteration) throws Exception {
    var realtimeEnv = envBuilder
      .addTrip(TRIP_INPUT, trip -> trip.withNetexAlteration(alteration))
      .build();
    var tripData = realtimeEnv.tripData(TRIP_ID);

    for (int stopPos = 0; stopPos < 3; stopPos++) {
      var call = new TripTimeOnDate(
        tripData.tripTimes(),
        stopPos,
        tripData.tripPattern(),
        SERVICE_DATE,
        MIDNIGHT
      );
      assertCancellation(true, false, call, realtimeEnv.transitService());
    }
  }

  @Test
  void tripCanceledInPlannedDataAndRealTime() throws Exception {
    var realtimeEnv = envBuilder
      .addTrip(TRIP_INPUT, trip -> trip.withNetexAlteration(TripAlteration.CANCELLATION))
      .build();
    var tripData = realtimeEnv.tripData(TRIP_ID);
    var pattern = tripData.tripPattern();
    var tripTimes = tripData.tripTimes().createRealTimeFromScheduledTimes().withCanceled().build();

    for (int stopPos = 0; stopPos < 3; stopPos++) {
      var call = new TripTimeOnDate(tripTimes, stopPos, pattern, SERVICE_DATE, MIDNIGHT);
      assertCancellation(true, true, call, realtimeEnv.transitService());
    }
  }

  @Test
  void flexTrip() {
    OTPFeature.FlexRouting.testOn(() -> {
      var realtimeEnv = envBuilder.addTrip(FLEX_TRIP_INPUT).build();
      var tripData = realtimeEnv.tripData(FLEX_TRIP_ID);
      var tripTimes = tripData.tripTimes();
      var pattern = tripData.tripPattern();

      var call = new TripTimeOnDate(tripTimes, 0, pattern, SERVICE_DATE, MIDNIGHT);

      var impl = new StopCallImpl();
      var env = DataFetchingSupport.dataFetchingEnvironment(
        call,
        Map.of(),
        realtimeEnv.transitService()
      );

      // using try-catch is not very nice here - ideally we would just add it to the method signature.
      // however, this test is run with the flex feature enabled in a runnable which prevents this.
      try {
        CallSchedule schedule = impl.schedule().get(env);
        assertEquals(
          new CallSchedule(new TimeWindow(TEN_AM, TEN_AM.plusMinutes(30)), false),
          schedule
        );
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * Assert the cancellation of a call in the planned data, in {@code schedule}, and by a real-time
   * update, in {@code realTime}. A call without real-time data is not canceled by a real-time
   * update.
   */
  private static void assertCancellation(
    boolean canceledInPlannedData,
    boolean canceledInRealTime,
    TripTimeOnDate call,
    TransitService transitService
  ) throws Exception {
    var impl = new StopCallImpl();
    var env = DataFetchingSupport.dataFetchingEnvironment(call, Map.of(), transitService);
    assertEquals(canceledInPlannedData, impl.schedule().get(env).canceled());
    var realTime = impl.realTime().get(env);
    assertEquals(canceledInRealTime, realTime != null && realTime.canceled());
  }
}
