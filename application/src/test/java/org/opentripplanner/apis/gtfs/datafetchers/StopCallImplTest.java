package org.opentripplanner.apis.gtfs.datafetchers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.model.CallSchedule;
import org.opentripplanner.apis.gtfs.model.CallScheduledTime.ArrivalDepartureTime;
import org.opentripplanner.apis.gtfs.model.CallScheduledTime.TimeWindow;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model._data.FlexTripInput;
import org.opentripplanner.transit.model._data.SiteTestBuilder;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.utils.time.ServiceDateUtils;

class StopCallImplTest implements RealtimeTestConstants {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2023, 6, 3);
  private static final ZoneId TIME_ZONE = ZoneId.of("Europe/Paris");
  private static final Instant MIDNIGHT = ServiceDateUtils.asStartOfService(
    SERVICE_DATE,
    TIME_ZONE
  ).toInstant();

  private static final OffsetDateTime NOON = OffsetDateTime.parse("2023-06-03T12:00+02:00");
  private static final OffsetDateTime TEN_AM = NOON.minusHours(2);
  private final SiteRepository site = SiteTestBuilder.of()
    .withStops(STOP_A_ID, STOP_B_ID, STOP_C_ID)
    .withAreaStops(STOP_D_ID, STOP_E_ID)
    .build();
  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of(
    site,
    SERVICE_DATE
  );

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A_ID, "12:00:00", "12:00:00")
    .addStop(STOP_B_ID, "12:30:00", "12:30:00")
    .addStop(STOP_C_ID, "13:00:00", "13:00:00")
    .build();

  private final FlexTripInput FLEX_TRIP_INPUT = FlexTripInput.of(TRIP_1_ID)
    .addStop(STOP_D_ID, "10:00", "10:30")
    .addStop(STOP_E_ID, "11:00", "11:30")
    .build();

  @Test
  void fixedTrip() throws Exception {
    var realtimeEnv = envBuilder.addTrip(TRIP_INPUT).build();
    var trip = realtimeEnv.tripFetcher(TRIP_1_ID);
    var tripTimes = trip.tripTimes();
    var pattern = trip.tripPattern();

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
      var trip = realtimeEnv.tripFetcher(TRIP_1_ID);
      var tripTimes = trip.tripTimes();
      var pattern = trip.tripPattern();

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
