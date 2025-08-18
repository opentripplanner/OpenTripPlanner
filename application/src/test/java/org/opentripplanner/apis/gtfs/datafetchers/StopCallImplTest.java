package org.opentripplanner.apis.gtfs.datafetchers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.model.CallSchedule;
import org.opentripplanner.apis.gtfs.model.CallScheduledTime.ArrivalDepartureTime;
import org.opentripplanner.apis.gtfs.model.CallScheduledTime.TimeWindow;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.FlexTripInput;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.RealtimeTestEnvironmentBuilder;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.utils.time.ServiceDateUtils;

class StopCallImplTest implements RealtimeTestConstants {

  private static final Instant MIDNIGHT = ServiceDateUtils.asStartOfService(
    SERVICE_DATE,
    TIME_ZONE
  ).toInstant();
  private static final OffsetDateTime NOON = OffsetDateTime.parse("2024-05-08T12:00+02:00");
  private static final OffsetDateTime TEN_AM = NOON.minusHours(2);
  private final RealtimeTestEnvironmentBuilder envBuilder = RealtimeTestEnvironment.of();
  private final RegularStop STOP_A = envBuilder.stop(STOP_A_ID);
  private final RegularStop STOP_B = envBuilder.stop(STOP_B_ID);
  private final RegularStop STOP_C = envBuilder.stop(STOP_C_ID);
  private final AreaStop STOP_D = envBuilder.areaStop(STOP_D_ID);
  private final AreaStop STOP_E = envBuilder.areaStop(STOP_E_ID);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A, "12:00:00", "12:00:00")
    .addStop(STOP_B, "12:30:00", "12:30:00")
    .addStop(STOP_C, "13:00:00", "13:00:00")
    .build();

  private final FlexTripInput FLEX_TRIP_INPUT = FlexTripInput.of(TRIP_1_ID)
    .addStop(STOP_D, "10:00", "10:30")
    .addStop(STOP_E, "11:00", "11:30")
    .build();

  @Test
  void fixedTrip() throws Exception {
    var realtimeEnv = envBuilder.addTrip(TRIP_INPUT).build();
    var tripTimes = realtimeEnv.getTripTimesForTrip(TRIP_1_ID);
    var pattern = realtimeEnv.getPatternForTrip(TRIP_1_ID);

    var call = new TripTimeOnDate(tripTimes, 0, pattern, SERVICE_DATE, MIDNIGHT);

    var impl = new StopCallImpl();
    var env = DataFetchingSupport.dataFetchingEnvironment(
      call,
      Map.of(),
      realtimeEnv.getTransitService()
    );

    var schedule = impl.schedule().get(env);
    assertEquals(new CallSchedule(new ArrivalDepartureTime(NOON, NOON)), schedule);
  }

  @Test
  void flexTrip() {
    OTPFeature.FlexRouting.testOn(() -> {
      var realtimeEnv = envBuilder.addFlexTrip(FLEX_TRIP_INPUT).build();
      var tripTimes = realtimeEnv.getTripTimesForTrip(TRIP_1_ID);
      var pattern = realtimeEnv.getPatternForTrip(TRIP_1_ID);

      var call = new TripTimeOnDate(tripTimes, 0, pattern, SERVICE_DATE, MIDNIGHT);

      var impl = new StopCallImpl();
      var env = DataFetchingSupport.dataFetchingEnvironment(
        call,
        Map.of(),
        realtimeEnv.getTransitService()
      );

      // using try-catch is not very nice here - ideally we would just add it to the method signature.
      // however, this test is run with the flex feature enabled in a runnable which prevents this.
      try {
        CallSchedule schedule = impl.schedule().get(env);
        assertEquals(new CallSchedule(new TimeWindow(TEN_AM, TEN_AM.plusMinutes(30))), schedule);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
