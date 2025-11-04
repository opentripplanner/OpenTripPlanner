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
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;
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
    assertEquals(new CallSchedule(new ArrivalDepartureTime(NOON, NOON)), schedule);
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
        assertEquals(new CallSchedule(new TimeWindow(TEN_AM, TEN_AM.plusMinutes(30))), schedule);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }
}
