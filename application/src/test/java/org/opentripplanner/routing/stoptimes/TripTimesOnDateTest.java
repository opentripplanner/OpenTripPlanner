package org.opentripplanner.routing.stoptimes;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.api.request.TripTimeOnDateRequest;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.utils.time.TimeUtils;

public class TripTimesOnDateTest implements RealtimeTestConstants {

  private static final TripInput TRIP_INPUT1 = TripInput
    .of("t1")
    .addStop(STOP_A1, "12:00", "12:01")
    .addStop(STOP_B1, "12:10", "12:11")
    .addStop(STOP_C1, "12:20", "12:21")
    .build();
  private static final TripInput TRIP_INPUT2 = TripInput
    .of("t2")
    .addStop(STOP_D1, "12:00", "12:01")
    .addStop(STOP_E, "12:10", "12:11")
    .addStop(STOP_F, "12:20", "12:21")
    .build();

  private static final TripInput TRIP_INPUT3 = TripInput
    .of("t3")
    .addStop(STOP_F, "12:15", "12:15")
    .addStop(STOP_E, "12:20", "12:20")
    .build();

  @Test
  void onFirstStop() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_INPUT1).addTrip(TRIP_INPUT2).build();

    var transitService = env.getTransitService();

    var instant = instant("12:00");
    {
      var result = StopTimesHelper.findTripTimeOnDate(
        transitService,
        TripTimeOnDateRequest.of(List.of(STOP_A1)).withTime(instant).build()
      );

      assertThat(result).hasSize(1);
      var tt = result.getFirst();
      assertEquals(instant("12:01"), tt.departure());
    }
    {
      var result = StopTimesHelper.findTripTimeOnDate(
        transitService,
        TripTimeOnDateRequest.of(List.of(STOP_B1)).withTime(instant).build()
      );
      assertThat(result).hasSize(1);
      var tt = result.getFirst();
      assertEquals(instant("12:11"), tt.departure());
    }
  }

  @Test
  void tooLate() {
    var transitService = RealtimeTestEnvironment
      .of()
      .addTrip(TRIP_INPUT1)
      .build()
      .getTransitService();

    var instant = instant("18:00");
    var result = StopTimesHelper.findTripTimeOnDate(
      transitService,
      TripTimeOnDateRequest.of(List.of(STOP_A1)).withTime(instant).build()
    );
    assertThat(result).isEmpty();
  }

  @Test
  void shortWindow() {
    var transitService = RealtimeTestEnvironment
      .of()
      .addTrip(TRIP_INPUT1)
      .build()
      .getTransitService();

    var instant = instant("11:00");
    var result = StopTimesHelper.findTripTimeOnDate(
      transitService,
      TripTimeOnDateRequest
        .of(List.of(STOP_A1))
        .withTime(instant)
        .withTimeWindow(Duration.ofMinutes(59))
        .build()
    );
    assertThat(result).isEmpty();
  }

  @Test
  void longerWindow() {
    var transitService = RealtimeTestEnvironment
      .of()
      .addTrip(TRIP_INPUT1)
      .build()
      .getTransitService();

    var instant = instant("11:00");
    var result = StopTimesHelper.findTripTimeOnDate(
      transitService,
      TripTimeOnDateRequest
        .of(List.of(STOP_A1))
        .withTime(instant)
        .withTimeWindow(Duration.ofMinutes(60))
        .build()
    );
    assertThat(result).isNotEmpty();
  }

  @Test
  void several() {
    var transitService = RealtimeTestEnvironment
      .of()
      .addTrip(TRIP_INPUT2, TRIP_INPUT3)
      .build()
      .getTransitService();

    var instant = instant("12:10");
    var result = StopTimesHelper.findTripTimeOnDate(
      transitService,
      TripTimeOnDateRequest
        .of(List.of(STOP_F))
        .withTime(instant)
        .withTimeWindow(Duration.ofMinutes(60))
        .build()
    );
    assertThat(result).hasSize(2);
  }

  private static Instant instant(String time) {
    var localTime = LocalTime.ofSecondOfDay(TimeUtils.time(time));
    return localTime.atDate(SERVICE_DATE).atZone(TIME_ZONE).toInstant();
  }
}
